package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import org.apache.logging.log4j.LogManager
import java.util.Objects
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("UnaryImageTransform")
abstract class UnaryImageTransform<TCanvasTeardownContext>(
    name: String,
    val base: AbstractImageTask,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
) : AbstractImageTask(name, cache, ctx, base.width, base.width) {
    override val directDependencies: List<AbstractImageTask> = listOf(base)
    override suspend fun renderOnto(contextSupplier: () -> GraphicsContext, x: Double, y: Double) {
        if (shouldRenderForCaching()) {
            super.renderOnto(contextSupplier, x, y)
        } else {
            renderOntoInternal(contextSupplier, x, y)
        }
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (other is UnaryImageTransform<*>) {
            base.mergeWithDuplicate(other.base)
        }
        return super.mergeWithDuplicate(other)
    }

    override suspend fun perform(): Image {
        val canvas by lazy(::createCanvas)
        renderOntoInternal({ canvas.graphicsContext2D }, 0.0, 0.0)
        return snapshotCanvas(canvas)
    }

    private suspend fun renderOntoInternal(context: () -> GraphicsContext, x: Double, y: Double) {
        ImageProcessingStats.onTaskLaunched(javaClass.simpleName, name)
        val ctx = context()
        try {
            val teardownContext = prepareContext(ctx)
            base.renderOnto({ ctx }, x, y)
            base.removeDirectDependentTask(this)
            unprepareContext(ctx, teardownContext)
        } catch (e: IllegalStateException) {
            logger.warn("Falling back to a second canvas", e)
            super.renderOnto({ ctx }, x, y)
        }
        ImageProcessingStats.onTaskCompleted(javaClass.simpleName, name)
    }

    protected abstract fun unprepareContext(ctx: GraphicsContext, teardownContext: TCanvasTeardownContext)

    protected abstract fun prepareContext(ctx: GraphicsContext): TCanvasTeardownContext

    override fun computeHashCode(): Int = Objects.hash(base, javaClass)

    override fun equals(other: Any?) = (this === other) || (other is UnaryImageTransform<*>
            && javaClass == other.javaClass
            && base == other.base)
}
