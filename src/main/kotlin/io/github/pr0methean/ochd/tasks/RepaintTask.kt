package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
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
): AbstractImageTask("{$base}@$paint@$alpha", cache, ctx, stats) {
    init {
        if (alpha == 1.0) {
            base.addOpaqueRepaint(this)
        }
    }

    override suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        if (alpha != 1.0 || isStartedOrAvailable() || mutex.withLock { directDependentTasks.size } > 1) {
            super.renderOnto(context, x, y)
        } else {
            logger.info("Rendering {} onto an existing canvas", name)
            stats.onTaskLaunched("RepaintTask", name)
            internalRenderOnto(context, x, y)
            stats.onTaskCompleted("RepaintTask", name)
        }
    }

    private suspend fun internalRenderOnto(context: GraphicsContext?, x: Double, y: Double): GraphicsContext {
        val drawStep: suspend (GraphicsContext) -> Unit
        val ctx: GraphicsContext
        if (context == null) {
            val baseImage = base.getNow()
            ?: base.opaqueRepaints().firstNotNullOfOrNull { task ->
                task.getNow()?.also { logger.info("Repainting $task for ${this@RepaintTask}") }
            }
            ?: base.await()
            base.removeDirectDependentTask(this)
            drawStep = { it.drawImage(baseImage, x, y) }
            logger.info("Allocating a canvas for {}", name)
            ctx = Canvas(baseImage.width, baseImage.height).graphicsContext2D
        } else {
            ctx = context
            drawStep = {
                base.renderOnto(it, x, y)
                base.removeDirectDependentTask(this)
            }
        }
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, ctx.canvas.width, ctx.canvas.height, paint)
            val blend = Blend()
            blend.mode = SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            ctx.setEffect(blend)
        }
        ctx.isImageSmoothing = false
        drawStep(ctx)
        ctx.setEffect(null)
        ctx.canvas.opacity = alpha
        return ctx
    }

    override fun startedOrAvailableSubtasks(): Int =
        if (isStartedOrAvailable()) {
            totalSubtasks
        } else if (base.isStartedOrAvailable() || base.opaqueRepaints().any(ImageTask::isStartedOrAvailable)) {
            base.totalSubtasks
        } else {
            base.startedOrAvailableSubtasks()
        }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is RepaintTask && other !== this && other.base !== base) {
            LOGGER.debug("Merging RepaintTask {} with duplicate {}", name, other.name)
            val newBase = base.mergeWithDuplicate(other.base)
            if (newBase !== base) {
                return RepaintTask(newBase, paint, alpha, cache, ctx, stats)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    override fun addOpaqueRepaint(repaint: ImageTask) {
        if (alpha == 1.0) {
            base.addOpaqueRepaint(repaint)
        }
        super.addOpaqueRepaint(repaint)
    }

    override suspend fun perform(): Image {
        stats.onTaskLaunched("RepaintTask", name)
        val gfx = internalRenderOnto(null, 0.0, 0.0)
        val canvas = gfx.canvas
        val output = WritableImage(canvas.width.toInt(), canvas.height.toInt())
        val snapshot = doJfx(name) {
            Platform.requestNextPulse()
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        }
        logger.info("Canvas is now unreachable for {}", name)
        if (snapshot.isError) {
            throw output.exception
        }
        stats.onTaskCompleted("RepaintTask", name)
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
