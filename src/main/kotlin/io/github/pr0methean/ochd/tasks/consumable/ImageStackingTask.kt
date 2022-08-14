package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asFlow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("ImageStackingTask")
class ImageStackingTask(val layers: LayerList,
                             val width: Int,
                             val height: Int,
                             override val name: String,
                             cache: TaskCache<Image>,
                             override val stats: ImageProcessingStats) : AbstractImageTask(name, cache ,stats) {
    init {
        if (layers.layers.isEmpty()) {
            throw IllegalArgumentException("Empty layer list")
        }
    }

    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped is ImageStackingTask && deduped !== this) {
            deduped.layers.mergeWithDuplicate(layers)
        }
        return deduped
    }

    override suspend fun clearFailure() {
        layers.layers.asFlow().collect(Task<Image>::clearFailure)
        super.clearFailure()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startPrerequisites() {
        layers.layers.asFlow().collect(Task<Image>::startAsync)
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
        val snapshotRef = AtomicReference<Image>(null)
        logger.debug("Creating layer tasks for {}", this)
        val layerRenderTasks = mutableListOf<Deferred<Result<Unit>>>()
        layers.layers.forEachIndexed { index, layerTask ->
            layerTask.startAsync()
            logger.debug("Creating consumer for layer {} ({})", index, layerTask)
            val previousLayerTask = layerRenderTasks.getOrNull(index - 1)
            val previousLayerName = layers.layers.getOrNull(index - 1).toString()
            layerRenderTasks.add(layerTask.consumeAsync {
                try {
                    previousLayerTask?.start()
                    logger.debug("Fetching layer {} ({})", index, layerTask)
                    val layerImage = it.getOrThrow()
                    logger.debug("Awaiting previous layer ({}) if needed", previousLayerName)
                    previousLayerTask?.await()?.getOrThrow()
                    logger.debug("Rendering layer {} ({}) onto the stack", box(index), layerTask)
                    canvasCtx.drawImage(layerImage, 0.0, 0.0)

                    if (index == layers.layers.lastIndex) {
                        val params = SnapshotParameters()
                        params.fill = layers.background
                        val output = WritableImage(width, height)
                        doJfx("Snapshot of $name") {
                            val snapshot = canvas.snapshot(params, output)
                            if (snapshot.isError) {
                                throw snapshot.exception
                            }
                            snapshotRef.set(snapshot)
                        }
                    }
                    logger.debug("Finished layer {} ({})", box(index), layerTask)
                    return@consumeAsync success(Unit)
                } catch (t: Throwable) {
                    logger.error("ImageStackingTask layer failed", t)
                    return@consumeAsync failure(t)
                }
            })
        }
        layerRenderTasks.forEach(Job::start)
        logger.debug("Waiting for layer tasks for {}", this)
        for (task in layerRenderTasks) {
            task.await()
        }
        val finalRenderTask = layerRenderTasks.last()
        finalRenderTask.await().getOrThrow()
        logger.debug("Layer tasks done for {}", this)
        stats.onTaskCompleted("ImageStackingTask", name)
        return snapshotRef.get()
    }
}