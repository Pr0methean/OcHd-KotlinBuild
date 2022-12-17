package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import kotlin.coroutines.CoroutineContext

abstract class AbstractImageTask(
    name: String, cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    open val stats: ImageProcessingStats
)
    : AbstractTask<Image>(name, cache, ctx), ImageTask {
    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask {
        return super.mergeWithDuplicate(other) as ImageTask
    }

    override suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        context.drawImage(await(), x, y)
    }

}
