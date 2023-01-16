package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import java.util.Collections
import java.util.WeakHashMap
import kotlin.coroutines.CoroutineContext

/** Specialization of [AbstractTask]&lt;[Image]&gt;. */
abstract class AbstractImageTask(
    name: String, cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    open val stats: ImageProcessingStats
)
    : AbstractTask<Image>(name, cache, ctx) {
    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is ImageTask) {
            other.opaqueRepaints().forEach(this::addOpaqueRepaint)
        }
        return super.mergeWithDuplicate(other) as AbstractImageTask
    }

    private val opaqueRepaints = Collections.newSetFromMap(WeakHashMap<ImageTask,Boolean>())

    open fun opaqueRepaints(): Iterable<ImageTask> = opaqueRepaints.toList()

    open fun addOpaqueRepaint(repaint: ImageTask) {
        opaqueRepaints.add(repaint)
    }

    open suspend fun renderOnto(context: GraphicsContext, x: Double, y: Double) {
        context.drawImage(await(), x, y)
    }

}
