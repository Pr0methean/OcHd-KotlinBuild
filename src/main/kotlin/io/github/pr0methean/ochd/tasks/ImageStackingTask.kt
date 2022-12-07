package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.application.Platform
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import org.apache.logging.log4j.LogManager
import java.util.*

private val logger = LogManager.getLogger("ImageStackingTask")

class ImageStackingTask(val layers: LayerList,
                        cache: TaskCache<Image>,
                        stats: ImageProcessingStats) : AbstractImageTask(layers.toString(), cache, stats) {
    @Suppress("MagicNumber")
    private val hashCode by lazy { layers.hashCode() + 37 }

    init {
        if (layers.layers.isEmpty()) {
            throw IllegalArgumentException("Empty layer list")
        }
    }

    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped !== other && deduped is ImageStackingTask && other is ImageStackingTask) {
            deduped.layers.mergeWithDuplicate(other.layers)
        }
        return deduped
    }

    override val directDependencies: List<ImageTask> = layers.layers

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingTask
                && other.layers == layers)
    }

    override fun hashCode(): Int = hashCode

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("ImageStackingTask", name)
        logger.debug("Fetching first layer of {} to check size", this)
        val firstLayer = layers.layers.first().await().getOrThrow()
        val width = firstLayer.width
        val height = firstLayer.height
        val canvas = Canvas(width, height)
        val canvasCtx = canvas.graphicsContext2D
        renderOntoInternal(canvasCtx, 0.0, 0.0) { canvasCtx.drawImage(firstLayer, 0.0, 0.0) }
        logger.debug("Taking snapshot of {}", name)
        val snapshot = takeSnapshot(width, height, canvas)
        stats.onTaskCompleted("ImageStackingTask", name)
        return snapshot
    }

    private suspend fun renderOntoInternal(
        canvasCtx: GraphicsContext,
        x: Double,
        y: Double,
        drawFirstLayer: suspend () -> Unit
    ) {
        if (layers.background != Color.TRANSPARENT) {
            canvasCtx.fill = layers.background
            canvasCtx.fillRect(0.0, 0.0, canvasCtx.canvas.width, canvasCtx.canvas.height)
        }
        drawFirstLayer()
        if (layers.layers.size > 1) {
            layers.layers.drop(1).forEach { it.renderOnto(canvasCtx, x, y) }
        }
    }

    override suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        if (isStartedOrAvailable() || cache.enabled) {
            super.renderOnto(context, x, y)
        } else {
            renderOntoInternal(context, x, y) { layers.layers[0].renderOnto(context, x, y) }
        }
    }

    private val background = layers.background

    private suspend fun takeSnapshot(
        width: Double,
        height: Double,
        canvas: Canvas
    ): Image {
        val params = SnapshotParameters()
        params.fill = background
        val output = WritableImage(width.toInt(), height.toInt())
        val snapshot = doJfx("Snapshot of $name") {
            Platform.requestNextPulse()
            canvas.snapshot(params, output)
        }
        if (snapshot.isError) {
            throw snapshot.exception
        }
        return snapshot
    }
}
