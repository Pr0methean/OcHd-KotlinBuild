package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.TextureTask
import io.github.pr0methean.ochd.tasks.TopPartCroppingTask
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

class LayerListBuilder(val ctx: ImageProcessingContext) {
    internal val layers = mutableListOf<TextureTask>()
    var background: Paint = Color.TRANSPARENT
    fun background(color: Paint) {
        background = color
    }
    fun background(red: Int, green: Int, blue: Int) {
        background = Color.rgb(red, green, blue)
    }
    fun background(color: Int) {
        background = c(color)
    }
    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): TextureTask {
        val layer = ctx.layer(name, paint, alpha)
        add(layer)
        return layer
    }
    fun copy(sourceInit: LayerListBuilder.() -> Unit) = copy(LayerListBuilder(ctx).also(sourceInit).build())
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
            add(ImageStackingTask(source, ctx))
        } else {
            addAll(source.layers)
        }
    }
    fun copyTopOf(source: TextureTask) = add(TopPartCroppingTask(source, ctx.tileSize, ctx))
    fun copyTopOf(source: LayerListBuilder.() -> Unit) = copyTopOf(ctx.stack(source))
    fun add(element: TextureTask) = layers.add(ctx.deduplicate(element))
    fun addAll(elements: Collection<TextureTask>) = layers.addAll(elements.map(ctx::deduplicate))
    fun build() = LayerList(layers.toList(), background, ctx)
}