package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.asFlow
import java.util.*
import java.util.concurrent.atomic.AtomicReference

data class ImageStackingConsumableTask(val layers: LayerList,
                                  val width: Int,
                                  val height: Int,
                                  override val name: String,
                                  val cache: TaskCache<Image>,
                                  override val stats: ImageProcessingStats) : AbstractConsumableImageTask(name, cache ,stats) {
    override suspend fun clearFailure() {
        layers.layers.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::clearFailure)
        super.clearFailure()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingConsumableTask
                && other.layers == layers
                && other.width == width
                && other.height == height)
    }

    override fun hashCode(): Int = Objects.hash(layers, width, height)

    override suspend fun perform(): Image {
        val canvas = Canvas(width.toDouble(), height.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        val layersList = layers.layers.map(ConsumableImageTask::unpacked)
        layersList.forEachIndexed { index, layerTask ->
            val previousLayerTask = layersList.getOrNull(index - 1)
            layerTask.consume {
                previousLayerTask?.await()
                doJfx("Layer $index of $name") {
                    if (index == 0) {
                        canvas.isCache = true
                        if (layers.background != Color.TRANSPARENT) {
                            canvasCtx.fill = layers.background
                            canvasCtx.fillRect(0.0, 0.0, width.toDouble(), height.toDouble())
                        }
                    }
                    canvasCtx.drawImage(it.getOrThrow(), 0.0, 0.0)
                }
            }
        }
        val snapshotRef = AtomicReference<Image>(null)
        layersList.last().consume {
            val output = WritableImage(width, height)
            doJfx("Snapshot of $name") {
                val snapshot = canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
                if (snapshot.isError) {
                    throw snapshot.exception
                }
                snapshotRef.set(snapshot)
            }
        }
        layersList.last().await()
        return snapshotRef.get()
    }
}