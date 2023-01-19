package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
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
    val base: AbstractImageTask,
    val paint: Paint?,
    val alpha: Double = 1.0,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    stats: ImageProcessingStats
): AbstractImageTask("{$base}@$paint@$alpha", cache, ctx, stats, base.width, base.height) {

    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (alpha != 1.0 || isStartedOrAvailable() || mutex.withLock { directDependentTasks.size } > 1) {
            super.renderOnto(contextSupplier, x, y)
        } else {
            stats.onTaskLaunched("RepaintTask", name)
            renderOntoInternal(contextSupplier, x, y)
            stats.onTaskCompleted("RepaintTask", name)
        }
    }

    private suspend fun renderOntoInternal(context: () -> GraphicsContext, x: Double, y: Double) {
        stats.onTaskLaunched("RepaintTask", name)
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
        ctx.canvas.opacity = alpha
        stats.onTaskCompleted("RepaintTask", name)
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is RepaintTask && other !== this && other.base !== base) {
            logger.debug("Merging RepaintTask {} with duplicate {}", name, other.name)
            val newBase = base.mergeWithDuplicate(other.base)
            if (newBase !== base) {
                return RepaintTask(newBase, paint, alpha, cache, ctx, stats)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    override suspend fun perform(): Image {
        val canvas by lazy(::createCanvas)
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0)
        val snapshot = snapshotCanvas(canvas)
        return snapshot
    }

    override val directDependencies: List<AbstractImageTask> = listOf(base)

    override fun computeHashCode(): Int = Objects.hash(base, paint, alpha)

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RepaintTask
                && other.base == base
                && other.paint == paint
                && other.alpha == alpha)
    }
}
