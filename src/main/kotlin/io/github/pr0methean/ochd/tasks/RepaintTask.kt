package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode.SRC_ATOP
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.util.Objects

private val logger = LogManager.getLogger("RepaintTask")

class RepaintTask(
    val base: ImageTask,
    val paint: Paint?,
    val alpha: Double = 1.0,
    cache: TaskCache<Image>,
    stats: ImageProcessingStats
): AbstractImageTask("{$base}@$paint@$alpha", cache, stats) {
    init {
        if (alpha == 1.0) {
            base.addOpaqueRepaint(this)
        }
    }

    override suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        if (cache.enabled || isStartedOrAvailable() || alpha != 1.0) {
            super.renderOnto(context, x, y)
        } else {
            logger.info("RepaintTask {} is drawing on an existing canvas", name)
            stats.onTaskLaunched("RepaintTask", name)
            internalRenderOnto(context, x, y)
            stats.onTaskCompleted("RepaintTask", name)
        }
    }

    private suspend fun internalRenderOnto(context: GraphicsContext?, x: Double, y: Double): GraphicsContext {
        // Determine whether we can repaint a repaint if it's available and the base image isn't
        val baseImageDeferred = base.getNow()
            ?: base.opaqueRepaints().firstNotNullOfOrNull { task ->
                task.getNow()?.also { logger.info("Repainting $task for ${this@RepaintTask}") }
            }
            ?: base.await()
        val baseImage = baseImageDeferred.getOrThrow()
        val ctx = context ?: Canvas(baseImage.width, baseImage.height).graphicsContext2D
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
