package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.isShallowCopyOf
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("ImageStackingTask")

/**
 * Task that superimposes multiple images onto a background.
 */
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class ImageStackingTask(
    val layers: LayerList,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    stats: ImageProcessingStats
) : AbstractImageTask(layers.toString(), cache, ctx, stats) {
    @Suppress("MagicNumber")
    override fun computeHashCode(): Int = layers.hashCode() + 37

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingTask
                && other.layers == layers)
    }

    init {
        require(layers.layers.isNotEmpty()) { "Empty layer list" }
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (this !== other && other is ImageStackingTask && other.layers !== layers) {
            logger.debug("Merging ImageStackingTask {} with duplicate {}", name, other.name)
            val mergedLayers = layers.mergeWithDuplicate(other.layers)
            if (!mergedLayers.layers.isShallowCopyOf(layers.layers)) {
                return ImageStackingTask(mergedLayers, cache, ctx, stats)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    override val directDependencies: List<AbstractImageTask> = layers.layers

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("ImageStackingTask", name)
        logger.debug("Fetching first layer of {} to check size", this)
        val firstLayer = layers.layers.first()
        val firstLayerImage = firstLayer.await()
        firstLayer.removeDirectDependentTask(this)
        val width = firstLayerImage.width
        val height = firstLayerImage.height
        logger.info("Allocating a canvas for {}", name)
        val canvas = Canvas(width, height)
        val canvasCtx = canvas.graphicsContext2D
        renderOntoInternal(canvasCtx, 0.0, 0.0) { canvasCtx.drawImage(firstLayerImage, 0.0, 0.0) }
        logger.debug("Taking snapshot of {}", name)
        val params = SnapshotParameters()
        params.fill = background
        val snapshot = snapshotCanvas(canvas, params)
        stats.onTaskCompleted("ImageStackingTask", name)
        return snapshot
    }

    private suspend inline fun renderOntoInternal(
        canvasCtx: GraphicsContext,
        x: Double,
        y: Double,
        drawFirstLayer: () -> Unit
    ) {
        if (layers.background != Color.TRANSPARENT) {
            canvasCtx.fill = layers.background
            canvasCtx.fillRect(0.0, 0.0, canvasCtx.canvas.width, canvasCtx.canvas.height)
        }
        drawFirstLayer()
        if (layers.layers.size > 1) {
            layers.layers.drop(1).forEach {
                it.renderOnto(canvasCtx, x, y)
                it.removeDirectDependentTask(this@ImageStackingTask)
            }
        }
    }

    override suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        if (isStartedOrAvailable() || mutex.withLock { directDependentTasks.size } > 1) {
            super.renderOnto(context, x, y)
        } else {
            logger.info("Rendering {} onto an existing canvas", name)
            val firstLayer = layers.layers[0]
            renderOntoInternal(context, x, y) {
                firstLayer.renderOnto(context, x, y)
                firstLayer.removeDirectDependentTask(this)
            }
        }
    }

    private val background = layers.background

}
