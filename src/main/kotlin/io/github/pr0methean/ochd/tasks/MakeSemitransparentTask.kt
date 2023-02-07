package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import java.util.Objects
import kotlin.coroutines.CoroutineContext

class MakeSemitransparentTask(
    base: AbstractImageTask,
    val opacity: Double,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
): UnaryImageTransform<Double>("$base@$opacity", base, cache, ctx) {

    constructor(base: AbstractImageTask, opacity: Double, cache: (String) -> DeferredTaskCache<Image>,
                ctx: CoroutineContext):
            this(base, opacity, cache("$base@$opacity"), ctx)
    override fun prepareContext(ctx: GraphicsContext): Double {
        val oldAlpha = ctx.globalAlpha
        ctx.globalAlpha = oldAlpha * opacity
        return oldAlpha
    }

    override fun unprepareContext(ctx: GraphicsContext, teardownContext: Double) {
        ctx.globalAlpha = teardownContext
    }

    override fun tryCombineWith(previousLayer: AbstractImageTask, ctx: TaskPlanningContext): List<AbstractImageTask> {
        if (previousLayer is MakeSemitransparentTask && previousLayer.opacity == opacity) {
            return listOf(ctx.layer(ctx.stack {
                copy(previousLayer.base)
                copy(base)
            }, alpha = opacity))
        }
        return super.tryCombineWith(previousLayer, ctx)
    }

    override fun hasColor(): Boolean = base.hasColor()

    override fun computeHashCode(): Int {
        return Objects.hash(super.computeHashCode(), opacity)
    }

    override fun equals(other: Any?): Boolean {
        return super.equals(other) && other is MakeSemitransparentTask && other.opacity == opacity
    }
}
