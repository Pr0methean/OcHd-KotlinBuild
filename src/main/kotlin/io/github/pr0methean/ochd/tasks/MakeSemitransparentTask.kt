package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import java.util.Objects
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

private val logger = LogManager.getLogger("UnaryImageTransform")
private const val ARGB_RGB_MASK = 1.shl(ARGB_ALPHA_BIT_SHIFT) - 1

@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class MakeSemitransparentTask(
    base: AbstractImageTask,
    val opacity: Double,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
): UnaryImageTransform<Double>("$base@$opacity", base, cache, ctx) {

    constructor(base: AbstractImageTask, opacity: Double, cache: (String) -> DeferredTaskCache<Image>,
                ctx: CoroutineContext):
            this(base, opacity, cache("$base@$opacity"), ctx)

    override suspend fun perform(): Image {
        ImageProcessingStats.onTaskLaunched("MakeSemitransparentTask", name)

        // repaintedForInputAlpha[it] = it * opacity
        val repaintedForInputAlpha = IntArray(1.shl(Byte.SIZE_BITS)) {
            (it * opacity).roundToInt().shl(ARGB_ALPHA_BIT_SHIFT)
        }

        /*
         * By doing the transformation ourselves rather than using JavaFX, we avoid allocating a Canvas and
         * waiting for the JavaFX renderer thread.
         */
        val input = base.await()
        base.removeDirectDependentTask(this)
        val reader = input.pixelReader
        logger.info("Allocating a WritableImage for Canvas-free transform of {} for {}", base.name, name)

        // Updating the image in place is sometimes possible, but for some reason it's slower
        val output = createWritableImage()
        val writer = output.pixelWriter

        for (y in 0 until height) {
            for (x in 0 until width) {
                // output pixel = input pixel * opacity
                val inputPixel = reader.getArgb(x, y)
                val inputAlpha = inputPixel.toUInt().shr(ARGB_ALPHA_BIT_SHIFT).toInt()
                val inputRgb = inputPixel.and(ARGB_RGB_MASK)
                writer.setArgb(x, y, repaintedForInputAlpha[inputAlpha].or(inputRgb))
            }
        }
        ImageProcessingStats.onTaskCompleted("MakeSemitransparentTask", name)
        return output
    }

    override fun prepareContext(ctx: GraphicsContext): Double {
        val oldAlpha = ctx.globalAlpha
        ctx.globalAlpha = oldAlpha * opacity
        return oldAlpha
    }

    override fun unprepareContext(ctx: GraphicsContext, teardownContext: Double) {
        ctx.globalAlpha = teardownContext
    }

    override fun tryCombineWith(previousLayer: AbstractImageTask, ctx: TaskPlanningContext): List<AbstractImageTask> {
        if (previousLayer is MakeSemitransparentTask && previousLayer.opacity == opacity) {
            return listOf(ctx.layer(ctx.stack {
                copy(previousLayer.base)
                copy(base)
            }, alpha = opacity))
        }
        if (previousLayer is RepaintTask && previousLayer.paint.opacity() == opacity) {
            return listOf(ctx.layer(ctx.stack {
                copy(previousLayer.base)
                copy(base)
            }, alpha = opacity))
        }
        return super.tryCombineWith(previousLayer, ctx)
    }

    override fun hasColor(): Boolean = base.hasColor()

    override fun computeHashCode(): Int {
        return Objects.hash(super.computeHashCode(), opacity)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is MakeSemitransparentTask && other.opacity == opacity
    }
}
