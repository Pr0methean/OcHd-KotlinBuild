package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.scene.paint.Paint

@OcHdDslMarker
class OutputTaskEmitter(private val ctx: TaskPlanningContext, private inline val emit: (PngOutputTask) -> Unit) {
    fun out(name: String, source: AbstractImageTask) {
        emit(ctx.out(name, source))
    }

    fun out(vararg names: String, source: AbstractImageTask) {
        emit(ctx.out(*names, source = source))
    }

    fun out(vararg names: String, sourceSvgName: String) {
        emit(ctx.out(*names, sourceSvgName = sourceSvgName))
    }

    fun out(name: String, sourceSvgName: String) {
        out(names = arrayOf(name), sourceSvgName = sourceSvgName)
    }

    fun out(vararg names: String, source: LayerListBuilder.() -> Unit) {
        emit(ctx.out(*names, source = ctx.stack(source)))
    }

    fun out(name: String, source: LayerListBuilder.() -> Unit) {
        out(names = arrayOf(name), source = source)
    }

    fun stack(layers: LayerList): AbstractImageTask = ctx.stack(layers)

    fun stack(init: LayerListBuilder.() -> Unit): AbstractImageTask = ctx.stack(init)

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): AbstractImageTask
            = ctx.layer(name, paint, alpha)

    fun layer(
        source: AbstractImageTask,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask = ctx.layer(source, paint, alpha)

    fun layer(
        source: LayerListBuilder.() -> Unit,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask = layer(stack {source()}, paint, alpha)

    fun animate(background: AbstractImageTask, frames: List<AbstractImageTask>): AbstractImageTask
            = ctx.animate(background, frames)
}
