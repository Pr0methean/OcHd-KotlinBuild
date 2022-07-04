package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.ImageStackingTask
import io.github.pr0methean.ochd.tasks.TextureTask
import io.github.pr0methean.ochd.tasks.TopPartCroppingTask
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

class LayerList(val ctx: ImageProcessingContext) : ArrayList<TextureTask>() {
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
    fun copy(sourceInit: LayerList.() -> Unit) = copy(LayerList(ctx).also(sourceInit))
    fun copy(source: LayerList) {
        if (source.background != Color.TRANSPARENT) {
            if (background == Color.TRANSPARENT && isEmpty()) {
                if (source.size <= 1) {
                    background = source.background
                }
            } else {
                throw IllegalStateException("Source's background would overwrite the existing layers")
            }
        }
        if (source.size > 1) {
            add(ImageStackingTask(source, ctx.tileSize, ctx))
        } else {
            addAll(source)
        }
    }
    fun copyTopOf(source: TextureTask) {
        add(TopPartCroppingTask(source, ctx.tileSize, ctx))
    }
    fun copyTopOf(source: LayerList.() -> Unit) = copyTopOf(ctx.stack(source))
    fun copyTopOf(source: LayerList) = copyTopOf {copy(source)}
    override fun add(element:TextureTask) = super.add(ctx.deduplicate(element))
    override fun addAll(elements: Collection<TextureTask>) = super.addAll(elements.map(ctx::deduplicate))
}