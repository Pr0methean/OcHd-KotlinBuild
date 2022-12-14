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
 * Task that recolors the input [Image] and/or makes it semitransparent. Has two special optimizations:
 * <ul>
 *     <li>If the base task isn't in the cache, but another repaint of it is and has alpha 1.0, we repaint the
 *     repaint rather than recreating the base image.</li>
 *     <li>When alpha is 1.0, and we're a non-cacheable task, we can render directly to the consuming task's canvas
 *     rather than allocating a {@link Canvas} and a {@link WritableImage}.</li>
 * </ul>
 * @param base the task whose output is recolored
 * @paint the image's new color
 * @alpha a multiplier applied to the image's opacity
 */
class RepaintTask(
    val base: ImageTask,
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
        // Determine whether we can repaint a repaint if it's available and the base image isn't
        val baseImage = base.getNow()
            ?: base.opaqueRepaints().firstNotNullOfOrNull { task ->
                task.getNow()?.also { logger.info("Repainting $task for ${this@RepaintTask}") }
            }
            ?: base.await()
        val ctx = context ?: Canvas(baseImage.width, baseImage.height).graphicsContext2D.also {
            logger.info("Allocating a canvas for {}", name)
        }
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, baseImage.width, baseImage.height, paint)
            val blend = Blend()
            blend.mode = SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            ctx.setEffect(blend)
        }
        ctx.isImageSmoothing = false
        ctx.drawImage(baseImage, x, y)
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

    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask {
        if (other is RepaintTask) {
            base.mergeWithDuplicate(other.base)
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

    override val directDependencies: List<ImageTask> = listOf(base)

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RepaintTask
                && other.base == base
                && other.paint == paint
                && other.alpha == alpha)
    }
    private val hashCode = Objects.hash(base, paint, alpha)
    override fun hashCode(): Int = hashCode
}
