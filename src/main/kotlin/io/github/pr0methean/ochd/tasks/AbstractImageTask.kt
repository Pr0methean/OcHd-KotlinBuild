package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

private val threadLocalCanvas = ThreadLocal.withInitial { Canvas(0.0, 0.0) }

/** Specialization of [AbstractTask]&lt;[Image]&gt;. */
abstract class AbstractImageTask(
    name: String, cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    open val stats: ImageProcessingStats
)
    : AbstractTask<Image>(name, cache, ctx) {
    protected fun getCanvas(width: Int, height: Int): Canvas = getCanvas(width.toDouble(), height.toDouble())

    protected fun getCanvas(width: Double, height: Double): Canvas {
        val canvas = threadLocalCanvas.get()
        canvas.width = width
        canvas.height = height
        return canvas
    }

    final override suspend fun perform(): Image = withContext(threadLocalCanvas.asContextElement()) {
        render()
    }

    abstract suspend fun render(): Image

    override suspend fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        return super.mergeWithDuplicate(other) as AbstractImageTask
    }

    open suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        context.drawImage(await(), x, y)
    }

}
