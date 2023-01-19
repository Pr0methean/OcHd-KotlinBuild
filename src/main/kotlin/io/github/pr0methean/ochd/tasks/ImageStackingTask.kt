package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.isShallowCopyOf
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
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
) : AbstractImageTask(layers.toString(), cache, ctx, stats, layers.width, layers.height) {
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
        val canvas by lazy(::createCanvas)
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0, layers.layers)
        logger.debug("Taking snapshot of {}", name)
        val params = SnapshotParameters()
        params.fill = background
        val snapshot = snapshotCanvas(canvas, params)
        return snapshot
    }

    private suspend fun renderOntoInternal(
        canvasCtxSupplier: () -> GraphicsContext,
        x: Double,
        y: Double,
        layers: List<AbstractImageTask>
    ) {
        stats.onTaskLaunched("ImageStackingTask", name)
        val canvasCtx by lazy(canvasCtxSupplier)
        layers.forEach {
            it.renderOnto({ canvasCtx }, x, y)
            it.removeDirectDependentTask(this@ImageStackingTask)
        }
        stats.onTaskCompleted("ImageStackingTask", name)
    }

    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (isStartedOrAvailable() || mutex.withLock { directDependentTasks.size } > 1) {
            super.renderOnto(contextSupplier, x, y)
        } else {
            renderOntoInternal(contextSupplier, x, y, layers.layers)
        }
    }

    private val background = layers.background
}
