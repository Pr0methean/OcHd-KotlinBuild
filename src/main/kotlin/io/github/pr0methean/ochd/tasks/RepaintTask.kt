package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.util.Objects
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

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
        name: String,
        base: AbstractImageTask,
        val paint: Paint,
        cache: DeferredTaskCache<Image>,
        ctx: CoroutineContext):
    UnaryImageTransform<Double>(name, base, cache, ctx) {

    constructor(base: AbstractImageTask, paint: Paint, cache: (String) -> DeferredTaskCache<Image>,
                ctx: CoroutineContext):
            this("{$base}@$paint", base, paint, cache, ctx)

    constructor(name: String,
                base: AbstractImageTask,
                paint: Paint,
                cache: (String) -> DeferredTaskCache<Image>,
                ctx: CoroutineContext):
            this(name, base, paint, cache(name), ctx)

    override fun appendForGraphPrinting(appendable: Appendable) {
        appendable.append('{')
        base.appendForGraphPrinting(appendable)
        appendable.append("}@").append(this.paint.toString())
    }

    @Suppress("DeferredResultUnused", "ComplexCondition", "MagicNumber")
    override suspend fun perform(): Image {
        val snapshot = if (paint is Color) {
            ImageProcessingStats.onTaskLaunched("RepaintTask", name)

            // paint's RGB channels as part of an ARGB int
            val rgb = (paint.red * 255).toInt().shl(16)
                .or((paint.green * 255).toInt().shl(8))
                .or((paint.blue * 255).toInt())

            // repaintedForInputAlpha[it] = paint * (it / 255)
            val repaintedForInputAlpha = IntArray(256) {
                (it * paint.opacity()).roundToInt().shl(24).or(rgb)
            }

            /*
             * By doing the transformation ourselves rather than using JavaFX, we avoid allocating a Canvas and
             * waiting for the JavaFX renderer thread.
             */
            val baseImage = base.await()
            base.removeDirectDependentTask(this)
            val width = baseImage.width.toInt()
            val height = baseImage.height.toInt()
            val reader = baseImage.pixelReader
            logger.info("Allocating a WritableImage for Canvas-free transform of {} for {}", base.name, name)

            // Updating the image in place is sometimes possible, but for some reason it's slower
            val output = WritableImage(width, height)
            val writer = output.pixelWriter

            // output pixel = repaintedForInputAlpha[255 * input pixel opacity] = paint * input pixel opacity
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val inputAlpha = reader.getArgb(x, y).toUInt().shr(24).toInt()
                    writer.setArgb(x, y, repaintedForInputAlpha[inputAlpha])
                }
            }
            ImageProcessingStats.onTaskCompleted("RepaintTask", name)
            output
        } else {
            super.perform()
        }
        if (paint is Color && paint.opacity == 1.0 && cache.isEnabled() && base.cache.isEnabled()
                && base.isUsedOnlyForRepaints()) {
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

    override fun hasColor(): Boolean = paint is Color && paint.toOpaque() == BLACK
    override fun prepareContext(ctx: GraphicsContext): Double {
        check(ctx.getEffect(null) == null) {"Can't chain $this onto ${ctx.getEffect(null)}"}
        val colorLayer = ColorInput(0.0, 0.0, ctx.canvas.width, ctx.canvas.height, paint.toOpaque())
        val blend = Blend()
        blend.mode = SRC_ATOP
        blend.topInput = colorLayer
        blend.bottomInput = null
        ctx.setEffect(blend)
        val oldAlpha = ctx.globalAlpha
        ctx.globalAlpha = oldAlpha * paint.opacity()
        return oldAlpha
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other !== this && other is UnaryImageTransform<*> && other.base !== base) {
            logger.debug("Merging RepaintTask {} with duplicate {}", name, other.name)
            val newBase = base.mergeWithDuplicate(other.base)
            if (newBase !== base) {
                return RepaintTask(name, newBase, paint, cache, ctx)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    override fun unprepareContext(ctx: GraphicsContext, teardownContext: Double) {
        ctx.globalAlpha = teardownContext
        ctx.setEffect(null)
    }

    override fun tryCombineWith(previousLayer: AbstractImageTask, ctx: TaskPlanningContext): List<AbstractImageTask> {
        if (previousLayer is RepaintTask && previousLayer.paint == paint) {
            return listOf(ctx.layer(ctx.stack {
                copy(previousLayer.base)
                copy(base)
            }, paint))
        }
        if (previousLayer is MakeSemitransparentTask && previousLayer.opacity == paint.opacity()) {
            return listOf(ctx.layer(ctx.stack {
                layer(previousLayer.base, paint.toOpaque())
                layer(base, paint.toOpaque())
            }, alpha = paint.opacity()))
        }
        return super.tryCombineWith(previousLayer, ctx)
    }

    override fun computeHashCode(): Int {
        return Objects.hash(super.computeHashCode(), paint)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is RepaintTask && other.paint == paint
    }
}

fun Paint.opacity(): Double {
    if (this.isOpaque) {
        return 1.0
    }
    if (this is Color) {
        return opacity
    }
    error("Can't determine opacity")
}

