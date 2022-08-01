package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.asFlow
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicReference

private val logger = LogManager.getLogger("ImageStackingTask")
class ImageStackingTask(val layers: LayerList,
                             val width: Int,
                             val height: Int,
                             override val name: String,
                             val cache: TaskCache<Image>,
                             override val stats: ImageProcessingStats) : AbstractConsumableImageTask(name, cache ,stats) {
    init {
        if (layers.layers.isEmpty()) {
            throw IllegalArgumentException("Empty layer list")
        }
    }
    override suspend fun clearFailure() {
        layers.layers.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::clearFailure)
        super.clearFailure()
    }

    override suspend fun startAsync(): Deferred<Result<Image>> {
        layers.layers.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::startAsync)
        return super.startAsync()
    }

    override suspend fun checkSanity() {
        layers.layers.asFlow().collect(ConsumableImageTask::checkSanity)
        super.checkSanity()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingTask
                && other.layers == layers
                && other.width == width
                && other.height == height)
    }

    override fun hashCode(): Int = Objects.hash(layers, width, height)

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("ImageStackingTask", name)
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        val layersList = layers.layers.map(ConsumableImageTask::unpacked)
        var previousLayerTask: Deferred<Unit>? = null
        val snapshotRef = AtomicReference<Image>(null)
        logger.debug("Creating layer tasks for {}", name)
        layersList.forEachIndexed { index, layerTask ->
            logger.debug("Creating consumer for layer $index ($layerTask)")
            val myPreviousLayerTask = previousLayerTask
            previousLayerTask = layerTask.consumeAsync {
                logger.debug("Fetching layer $index ($layerTask)")
                myPreviousLayerTask?.start()
                val layerImage = it.getOrThrow()
                myPreviousLayerTask?.await()
                logger.debug("Rendering layer $index ($layerTask) onto the stack")
                doJfx("Layer $index: $layerTask") {
                    if (index == 0) {
                        canvas.isCache = true
                        if (layers.background != Color.TRANSPARENT) {
                            canvasCtx.fill = layers.background
                            canvasCtx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
                        }
                    }
                    canvasCtx.drawImage(layerImage, 0.0, 0.0)
                }
                if (index == layersList.lastIndex) {
                    val output = WritableImage(width, height)
                    doJfx("Snapshot of $name") {
                        val snapshot = canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
                        if (snapshot.isError) {
                            throw snapshot.exception
                        }
                        snapshotRef.set(snapshot)
                    }
                }
            }
            layerTask.startAsync()
        }
        previousLayerTask!!.start()
        logger.debug("Waiting for layer tasks for {}", name)
        previousLayerTask!!.await()
        stats.onTaskCompleted("ImageStackingConsumableTask", name)
        return snapshotRef.get()
    }
}