package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.TextureTask
import io.github.pr0methean.ochd.tasks.TopPartCroppingTask
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

class LayerListBuilder(val ctx: ImageProcessingContext) {
    internal val layers = mutableListOf<TextureTask>()
    var background: Paint = Color.TRANSPARENT
    fun background(color: Color, opacity: Double = 1.0) {
        background = if (opacity == 1.0) color else Color(color.red, color.green, color.blue, opacity * color.opacity)
    }
    fun background(red: Int, green: Int, blue: Int) {
        background = Color.rgb(red, green, blue)
    }
    fun background(color: Int) {
        background = c(color)
    }
    fun background(paint: Paint) {
        background = paint
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    fun layer(source: TextureTask, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    fun copy(sourceInit: LayerListBuilder.() -> Unit) =
        copy(LayerListBuilder(ctx).also(sourceInit).build())
    fun copy(source: LayerList) {
        if (source.background != Color.TRANSPARENT) {
            if (background == Color.TRANSPARENT && layers.isEmpty()) {
                if (source.layers.size <= 1) {
                    background = source.background
                }
            } else {
                throw IllegalStateException("Source's background would overwrite the existing layers")
            }
        }
        if (source.layers.size > 1) { // Don't flatten sub-stacks since we want to deduplicate them
            copy(ctx.stack { addAll(source.layers) })
        } else {
            addAll(source.layers)
        }
    }
    fun copy(source: SingleTextureMaterial) 
            = copy(LayerListBuilder(ctx).also {source.run {createTextureLayers()}}.build())
    fun copyTopOf(source: TextureTask) = copy(TopPartCroppingTask(source, ctx.tileSize, ctx.packer, ctx.scope, ctx.stats, ctx.retryer))
    fun copyTopOf(source: LayerListBuilder.() -> Unit) = copyTopOf(ctx.stack(source))
    fun copy(element: TextureTask) = layers.add(ctx.deduplicate(element))
    fun addAll(elements: Collection<TextureTask>) = layers.addAll(elements.map(ctx::deduplicate))
    fun build() = LayerList(layers.toList(), background)
}