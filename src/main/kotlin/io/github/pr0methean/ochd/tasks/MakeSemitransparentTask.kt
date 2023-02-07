package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
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

    override fun hasColor(): Boolean = base.hasColor()
}
