package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.asFlow
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox.box
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("ImageStackingTask")
class ImageStackingTask(val layers: LayerList,
                        name: String,
                        cache: TaskCache<Image>,
                        stats: ImageProcessingStats) : AbstractImageTask(name, cache, stats) {
    init {
        if (layers.layers.isEmpty()) {
            throw IllegalArgumentException("Empty layer list")
        }
    }

    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped !== other && deduped is ImageStackingTask && other is ImageStackingTask) {
            deduped.layers.mergeWithDuplicate(other.layers)
        }
        return deduped
    }

    override val directDependencies: List<ImageTask> = layers.layers

    override suspend fun clearFailure() {
        layers.layers.asFlow().collect(Task<Image>::clearFailure)
        super.clearFailure()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingTask
                && other.layers == layers)
    }

    override fun hashCode(): Int = layers.hashCode() + 37

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("ImageStackingTask", name)
        logger.debug("Fetching first layer of {} to check size", this)
        val firstLayer = layers.layers.first().await().getOrThrow()
        val width = firstLayer.width
        val height = firstLayer.height
        val canvas = Canvas(width, height)
        val canvasCtx = canvas.graphicsContext2D
        val snapshotRef = AtomicReference<Image>(null)
        logger.debug("Creating layer tasks for {}", this)
        val layerRenderTasks = mutableListOf<Deferred<Result<Unit>>>()
        layerRenderTasks.add(createCoroutineScope().async {
            logger.debug("Rendering first layer ({}) of {}", firstLayer, this@ImageStackingTask)
            canvasCtx.drawImage(firstLayer, 0.0, 0.0)
            if (layers.layers.size == 1) {
                takeSnapshot(width, height, canvas, snapshotRef)
            }
            return@async success(Unit)
        })
        layers.layers.withIndex().drop(1).forEach { (index, layerTask) ->
            logger.debug("Creating consumer for layer {} ({})", index, layerTask)
            val previousLayerTask = layerRenderTasks.getOrNull(index - 1)
            val previousLayerName = layers.layers.getOrNull(index - 1).toString()
            layerRenderTasks.add(layerTask.consumeAsync {
                try {
                    logger.debug("Awaiting previous layer ({}) if needed", previousLayerName)
                    previousLayerTask?.await()?.getOrThrow()
                    logger.debug("Fetching layer {} ({})", index, layerTask)
                    val layerImage = it.getOrThrow()
                    logger.debug("Rendering layer {} ({}) onto the stack", box(index), layerTask)
                    canvasCtx.drawImage(layerImage, 0.0, 0.0)
                    logger.debug("Finished layer {} ({})", box(index), layerTask)
                    if (index == layers.layers.lastIndex) {
                        takeSnapshot(width, height, canvas, snapshotRef)
                    }
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
            task.await().getOrThrow()
        }
        logger.debug("Layer tasks done for {}", this)
        stats.onTaskCompleted("ImageStackingTask", name)
        return snapshotRef.getAndSet(null)
    }

    private suspend fun takeSnapshot(
        width: Double,
        height: Double,
        canvas: Canvas,
        snapshotRef: AtomicReference<Image>
    ) {
        val params = SnapshotParameters()
        params.fill = layers.background
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