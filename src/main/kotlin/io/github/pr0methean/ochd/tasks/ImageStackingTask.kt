package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicReference

private val logger = LogManager.getLogger("ImageStackingTask")

class ImageStackingTask(val layers: LayerList,
                        cache: TaskCache<Image>,
                        stats: ImageProcessingStats) : AbstractImageTask(layers.toString(), cache, stats) {
    private val hashCode by lazy {layers.hashCode() + 37}
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
        val snapshotRef = AtomicReference<Image>(null)
        val firstLayer = layers.layers.first().await().getOrThrow()
        val width = firstLayer.width
        val height = firstLayer.height
        val canvas = Canvas(width, height)
        val canvasCtx = canvas.graphicsContext2D
        canvasCtx.drawImage(firstLayer, 0.0, 0.0)
        if (layers.layers.size > 1) {
            layers.layers.drop(1).forEach { layerTask ->
                val layerImage = layerTask.await().getOrThrow()
                logger.debug("Rendering {} onto the stack", layerTask)
                canvasCtx.drawImage(layerImage, 0.0, 0.0)
                logger.debug("Finished rendering {}", layerTask)
            }
        }
        logger.debug("Taking snapshot of {}", name)
        takeSnapshot(width, height, canvas, snapshotRef)
        stats.onTaskCompleted("ImageStackingTask", name)
        return snapshotRef.getAndSet(null)
    }

    private val background = layers.background

    private suspend fun takeSnapshot(
        width: Double,
        height: Double,
        canvas: Canvas,
        snapshotRef: AtomicReference<Image>
    ) {
        val params = SnapshotParameters()
        params.fill = background
        val output = WritableImage(width.toInt(), height.toInt())
        val snapshot = doJfx("Snapshot of $name") {
            canvas.snapshot(params, output)
        }
        if (snapshot.isError) {
            throw snapshot.exception
        }
        snapshotRef.set(snapshot)
    }

}