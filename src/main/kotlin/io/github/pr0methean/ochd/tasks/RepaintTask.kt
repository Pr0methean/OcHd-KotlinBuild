package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.scene.effect.Blend
import javafx.scene.effect.BlendMode
import javafx.scene.effect.ColorInput
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Paint
import org.apache.logging.log4j.LogManager
import java.util.*

private val logger = LogManager.getLogger("RepaintTask")
class RepaintTask(
    val base: ImageTask,
    val paint: Paint?,
    val alpha: Double = 1.0,
    cache: TaskCache<Image>,
    stats: ImageProcessingStats
): AbstractImageTask("$base@$paint@$alpha", cache, stats) {
    init {
        if (alpha == 1.0) {
            base.addOpaqueRepaint(this)
        }
    }

    override fun uncachedSubtasks(): Int {
        val possiblyUncached = super.uncachedSubtasks()
        if (possiblyUncached <= 1) {
            return possiblyUncached
        }
        for (repaint in base.opaqueRepaints()) {
            if (repaint.getNow() != null) {
                return 1
            }
        }
        return possiblyUncached
    }

    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
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
        var baseImage: Image? = base.getNow()?.getOrThrow()
        if (baseImage == null) {
            for (repaint in base.opaqueRepaints()) {
                val repaintNow = repaint.getNow()
                if (repaintNow != null) {
                    logger.info("Repainting {} to create {}", repaint, this)
                    baseImage = repaintNow.getOrThrow()
                    break
                }
            }
        }
        baseImage = baseImage ?: base.await().getOrThrow()
        stats.onTaskLaunched("RepaintTask", name)
        val canvas = createCanvas(baseImage.width, baseImage.height, name)
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
            awaitFreeMemory(4 * baseImage.width.toLong() * baseImage.height.toLong(), name)
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
            if (output.isError) {
                throw output.exception
            }
            return@doJfx output
        }
        stats.onTaskCompleted("RepaintTask", name)
        return snapshot
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is RepaintTask
                && other.base == base
                && other.paint == paint
                && other.alpha == alpha)
    }

    override fun hashCode(): Int {
        return Objects.hash(base, paint, alpha)
    }
}