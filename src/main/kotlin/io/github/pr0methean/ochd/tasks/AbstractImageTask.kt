package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.SnapshotParameters
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import org.apache.logging.log4j.LogManager
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("AbstractImageTask")

/** Specialization of [AbstractTask]&lt;[Image]&gt;. */
abstract class AbstractImageTask(
    name: String, cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    open val stats: ImageProcessingStats
)
    : AbstractTask<Image>(name, cache, ctx) {
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        return super.mergeWithDuplicate(other) as AbstractImageTask
    }

    open suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        context.drawImage(await(), x, y)
    }

    protected suspend fun snapshotCanvas(canvas: Canvas, params: SnapshotParameters = DEFAULT_SNAPSHOT_PARAMS): Image {
        val output = WritableImage(canvas.width.toInt(), canvas.height.toInt())
        val snapshot = doJfx("Snapshot of $name") {
            canvas.snapshot(params, output)
        }
        if (snapshot.isError) {
            throw output.exception
        }
        logger.info("Canvas is now unreachable for {}", name)
        return snapshot
    }
}
