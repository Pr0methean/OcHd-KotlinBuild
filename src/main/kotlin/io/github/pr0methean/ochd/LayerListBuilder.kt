package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.RepaintTask
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

    @Suppress("ComplexCondition")
    private fun addDeduplicatedLayer(layer: AbstractImageTask) {
        val currentTop = layers.lastOrNull()
        if (layer is RepaintTask && currentTop is RepaintTask
                && layer.paint == currentTop.paint
                && layer.alpha == currentTop.alpha) {
            layers.removeLast()
            val combinedRepaint = ctx.layer(ctx.stack(
                    LayerList(listOf(currentTop.base, layer.base), Color.TRANSPARENT)),
                    layer.paint, layer.alpha)
            layers.add(combinedRepaint)
        } else {
            layers.add(layer)
        }
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0) {
        val layer = ctx.layer(name, paint, alpha)
        addDeduplicatedLayer(layer)
    }

    fun layer(
        source: AbstractImageTask, paint: Paint? = null, alpha: Double = 1.0
    ) {
        val layer = ctx.layer(source, paint, alpha)
        addDeduplicatedLayer(layer)
    }

    inline fun copy(sourceInit: LayerListBuilder.() -> Unit) =
        copy(LayerListBuilder(ctx).apply(sourceInit).build())

    fun copy(source: LayerList) {
        check(source.layers.isNotEmpty()) { "Copying from empty LayerList" }
        if (source.background != Color.TRANSPARENT) {
            check(background == Color.TRANSPARENT) { "Source's background would overwrite an existing background" }
            check(layers.isEmpty()) { "Source's background would overwrite an existing layer" }
            if (source.layers.size == 1) {
                background = source.background
            }
        }
        if (source.layers.size > 1) { // Don't flatten sub-stacks since we want to deduplicate them
            addDeduplicatedLayer(ctx.stack(source))
        } else {
            copy(source.layers[0])
        }
    }
    fun copy(source: SingleTextureMaterial): Unit = source.copyTo(this)

    fun copy(element: AbstractImageTask): Boolean {
        return layers.add(ctx.deduplicate(element))
    }

    fun build(): LayerList {
        check(layers.isNotEmpty()) { "Trying to create an empty LayerList" }
        return LayerList(layers, background)
    }
}
