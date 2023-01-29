package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.paint.Paint
import kotlinx.coroutines.CompletableDeferred
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
    val paint: Paint?,
    alpha: Double = 1.0,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
): AbstractImageTask("{$base}@$paint@$alpha", cache, ctx, base.width, base.height) {

    val base: AbstractImageTask
    val alpha: Double

    init {
        var realBase = base
        var realAlpha = 1.0
        while (realBase is RepaintTask) {
            realAlpha *= realBase.alpha
            realBase = realBase.base
        }
        this.base = realBase
        this.alpha = realAlpha
    }

    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (alpha != 1.0 || shouldRenderForCaching()) {
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
        ctx.canvas.opacity = alpha
        ImageProcessingStats.onTaskCompleted("RepaintTask", name)
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is RepaintTask && other !== this && other.base !== base) {
            logger.debug("Merging RepaintTask {} with duplicate {}", name, other.name)
            val newBase = base.mergeWithDuplicate(other.base)
            if (newBase !== base) {
                return RepaintTask(newBase, paint, alpha, cache, ctx)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        val canvas by lazy(::createCanvas)
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0)
        val snapshot = snapshotCanvas(canvas)
        if (alpha == 1.0 && base.cache.isEnabled() && base.getNow() == null
                && base.directDependentTasks.all { it is RepaintTask }) {
            base.cache.computeIfAbsent {
                logger.info("Using repaint {} to replace base image {}", name, base.name)
                CompletableDeferred(snapshot)
            }
        }
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
