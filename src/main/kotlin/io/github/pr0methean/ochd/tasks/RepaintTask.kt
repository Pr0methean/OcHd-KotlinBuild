package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.application.Platform
import javafx.scene.canvas.Canvas
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.selects.select
import java.util.Objects

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
        val baseImageResult = base.getNow() ?:
                base.opaqueRepaints().firstNotNullOfOrNull { it.getNow() } ?:
                select<Result<Image>> {
                    var started = false
                    (base.opaqueRepaints() + base).forEach {
                        // identityHashCode comparison prevents 2 RepaintTask with same base from waiting on each other
                        if (System.identityHashCode(it) < System.identityHashCode(this@RepaintTask)) {
                            it.coroutine()?.also { started = true }?.onAwait
                        }
                    }
                    if (!started) {
                        runBlocking { base.startAsync() }.onAwait
                    }
                }
        val baseImage = baseImageResult.getOrThrow()
        stats.onTaskLaunched("RepaintTask", name)
        val canvas = Canvas(baseImage.width, baseImage.height)
        val output = WritableImage(baseImage.width.toInt(), baseImage.height.toInt())
        val gfx = canvas.graphicsContext2D
        canvas.opacity = alpha
        if (paint != null) {
            val colorLayer = ColorInput(0.0, 0.0, baseImage.width, baseImage.height, paint)
            val blend = Blend()
            blend.mode = BlendMode.SRC_ATOP
            blend.topInput = colorLayer
            blend.bottomInput = null
            gfx.setEffect(blend)
        }
        gfx.isImageSmoothing = false
        gfx.drawImage(baseImage, 0.0, 0.0)
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
