package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

@OcHdDslMarker
class LayerListBuilder(val ctx: TaskPlanningContext) {
    private val layers: MutableList<AbstractImageTask> = mutableListOf()
    var background: Paint = Color.TRANSPARENT
    fun background(paint: Paint, opacity: Double = 1.0) {
        background = if (opacity == 1.0 || paint !is Color) {
            paint
        } else {
            Color(paint.red, paint.green, paint.blue, opacity * paint.opacity)
        }
    }
    fun background(red: Int, green: Int, blue: Int) {
        background = Color.rgb(red, green, blue)
    }
    fun background(color: Int) {
        background = c(color)
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): AbstractImageTask {
        val layer = ctx.layer(name, paint, alpha)
        addAll(listOf(layer))
        return layer
    }

    fun layer(
        source: AbstractImageTask,
        paint: Paint? = null,
        alpha: Double = 1.0
    ): AbstractImageTask {
        val layer = ctx.layer(source, paint, alpha)
        addAll(listOf(layer))
        return layer
    }

    inline fun copy(sourceInit: LayerListBuilder.() -> Unit): Unit =
        copy(LayerListBuilder(ctx).also {sourceInit()}.build())
    fun copy(source: LayerList) {
        if (source.background != Color.TRANSPARENT) {
            check(background == Color.TRANSPARENT) { "Source's background would overwrite an existing background" }
            check(layers.isEmpty()) { "Source's background would overwrite an existing layer" }
            background = source.background
        }
        if (source.layers.size > 1) { // Don't flatten sub-stacks since we want to deduplicate them
            copy(ctx.stack(source))
        } else {
            addAll(listOf(ctx.deduplicate(source.layers[0])))
        }
    }
    fun copy(source: SingleTextureMaterial): Unit = copy(LayerListBuilder(ctx).also {
        source.run {createTextureLayers()}
    }.build())

    fun copy(element: AbstractImageTask): Boolean {
        return layers.add(ctx.deduplicate(element))
    }
    private fun addAll(elements: Collection<AbstractImageTask>): Boolean = layers.addAll(elements)
    fun build(): LayerList = LayerList(layers, background)
}
