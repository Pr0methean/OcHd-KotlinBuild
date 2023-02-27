package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.isShallowCopyOf
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.TRANSPARENT
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("ImageStackingTask")

/**
 * Task that superimposes multiple images onto a background in order.
 */
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class ImageStackingTask(
    val layers: LayerList,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
) : AbstractImageTask(layers.toString(), cache, ctx, layers.width, layers.height) {
    @Suppress("MagicNumber")
    override fun computeHashCode(): Int = layers.hashCode() + 37

    override fun equals(other: Any?): Boolean {
        return (this === other) || (other is ImageStackingTask
                && other.layers == layers)
    }

    // An ImageStackingTask that contains a RepaintTask is too long for one line on the graph.
    override fun appendForGraphPrinting(appendable: Appendable) {
        if (layers.layers.any { it is RepaintTask || it is ImageStackingTask }) {
            appendable.append(layers.background.toString())
            layers.layers.forEach {
                appendable.append(",\\n")
                it.appendForGraphPrinting(appendable)
            }
        } else name
    }

    init {
        require(layers.layers.isNotEmpty()) { "Empty layer list" }
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (this !== other && other is ImageStackingTask && other.layers !== layers) {
            logger.debug("Merging ImageStackingTask {} with duplicate {}", name, other.name)
            val mergedLayers = layers.mergeWithDuplicate(other.layers)
            if (!mergedLayers.layers.isShallowCopyOf(layers.layers)) {
                return ImageStackingTask(mergedLayers, cache, ctx)
            }
        }
        return super.mergeWithDuplicate(other)
    }

    override val directDependencies: List<AbstractImageTask> = layers.layers

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        val canvas = createCanvas()
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0, layers.layers)
        logger.debug("Taking snapshot of {}", name)
        val params = SnapshotParameters()
        params.fill = layers.background
        return snapshotCanvas(canvas, params)
    }

    private suspend fun renderOntoInternal(
        canvasCtxSupplier: () -> GraphicsContext,
        x: Double,
        y: Double,
        layers: List<AbstractImageTask>
    ) {
        ImageProcessingStats.onTaskLaunched("ImageStackingTask", name)
        if (this.layers.background != TRANSPARENT) {
            val context = canvasCtxSupplier()
            context.fill = this.layers.background
            context.fillRect(0.0, 0.0, context.canvas.width, context.canvas.height)
        }
        layers.forEach {
            it.renderOnto(canvasCtxSupplier, x, y)
            it.removeDirectDependentTask(this@ImageStackingTask)
        }
        ImageProcessingStats.onTaskCompleted("ImageStackingTask", name)
    }

    override fun hasColor(): Boolean {
        return (layers.background != TRANSPARENT && layers.background.toOpaque() != BLACK)
                || layers.layers.any(AbstractImageTask::hasColor)
    }

    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (shouldRenderForCaching()) {
            super.renderOnto(contextSupplier, x, y)
        } else {
            renderOntoInternal(contextSupplier, x, y, layers.layers)
        }
    }

}
