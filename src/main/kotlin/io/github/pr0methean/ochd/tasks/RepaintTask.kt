package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.Objects
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("RepaintTask")

/**
 * Task that recolors the input [Image] and/or makes it semitransparent. Has one special optimization:
 * <ul>
 *     <li>When alpha is 1.0, and we're a non-cacheable task, we can render directly to the consuming task's canvas
 *     rather than allocating a {@link Canvas} and a {@link WritableImage}.</li>
 * </ul>
 * @param base the task whose output is recolored
 * @paint the image's new color
 * @alpha a multiplier applied to the image's opacity
 */
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class RepaintTask(
    base: AbstractImageTask,
    paint: Paint?,
    alpha: Double = 1.0,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
): AbstractImageTask("{$base}@$paint", cache, ctx, base.width, base.height) {

    val base: AbstractImageTask
    val paint: Paint?

    init {
        var realBase = base
        var realAlpha = 1.0
        while (realBase is RepaintTask) {
            realAlpha *= (realBase.paint as? Color)?.opacity ?: 1.0
            realBase = realBase.base
        }
        this.paint = if (alpha == 1.0) {
            paint
        } else if (paint is Color) {
            Color(paint.red, paint.green, paint.blue, paint.opacity * realAlpha)
        } else error("Can't implement transparency")
        this.base = realBase
    }

    override val nameForGraphPrinting: String by lazy { buildString {
        val baseName = base.nameForGraphPrinting
        append('{').append(baseName).append('}')
        if (baseName.contains("\\n")) {
            append("\\n")
        }
        append('@').append(paint).append('@').append(alpha)
    } }

    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (shouldRenderForCaching() || (paint is Color && paint.opacity != 1.0)) {
            super.renderOnto(contextSupplier, x, y)
        } else {
            renderOntoInternal(contextSupplier, x, y)
        }
    }

    private suspend fun renderOntoInternal(context: () -> GraphicsContext, x: Double, y: Double) {
        ImageProcessingStats.onTaskLaunched("RepaintTask", name)
        val ctx = context()
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, ctx.canvas.width, ctx.canvas.height, paint)
            val blend = Blend()
            blend.mode = SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            ctx.setEffect(blend)
        }
        base.renderOnto({ ctx }, x, y)
        base.removeDirectDependentTask(this)
        ctx.setEffect(null)
        ImageProcessingStats.onTaskCompleted("RepaintTask", name)
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is RepaintTask && other !== this && other.base !== base) {
            logger.debug("Merging RepaintTask {} with duplicate {}", name, other.name)
            val newBase = base.mergeWithDuplicate(other.base)
            if (newBase !== base) {
                return RepaintTask(newBase, paint, 1.0, cache, ctx)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("DeferredResultUnused", "ComplexCondition")
    override suspend fun perform(): Image {
        val canvas by lazy(::createCanvas)
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0)
        val snapshot = snapshotCanvas(canvas)
        if (paint is Color && paint.opacity == 1.0 && cache.isEnabled() && base.cache.isEnabled()
                && base.mutex.withLock { base.directDependentTasks.all { it is RepaintTask } }) {
            /*
             * Painting a black X blue or green has the same result as painting a red X blue or green.
             * Therefore, if the only impending uses of a black X are to repaint it blue and green, we can replace the
             * black X with a red one in cache, if the red one was going to be cached as a pre-painted layer anyway.
             */
            logger.info("Using repaint {} to replace base image {}", name, base.name)
            base.cache.setValue(snapshot)
        }
        return snapshot
    }

    override val directDependencies: List<AbstractImageTask> = listOf(base)

    override fun computeHashCode(): Int = Objects.hash(base, paint)

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RepaintTask
                && other.base == base
                && other.paint == paint)
    }
}
