package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.ConsumableImageTask
import io.github.pr0methean.ochd.tasks.consumable.ConsumableTask
import io.github.pr0methean.ochd.tasks.consumable.TopPartCroppingTask
import io.github.pr0methean.ochd.tasks.consumable.caching.NoopTaskCache
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

class LayerListBuilder(val ctx: ImageProcessingContext) {
    internal val layers = mutableListOf<ConsumableImageTask>()
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

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    fun layer(source: ConsumableTask<Image>, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    fun layer(source: ConsumableImageTask, paint: Paint? = null, alpha: Double = 1.0) = layer(source.unpacked, paint, alpha)

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
    fun copyTopOf(source: ConsumableTask<Image>) = copy(TopPartCroppingTask(source, "Top part of $source", NoopTaskCache(), ctx.stats))
    fun copyTopOf(source: ConsumableImageTask) = copyTopOf(source.unpacked)
    fun copyTopOf(source: LayerListBuilder.() -> Unit) = copyTopOf(ctx.stack(source))
    fun copy(element: ConsumableImageTask) = layers.add(ctx.deduplicate(element))
    fun addAll(elements: Collection<ConsumableImageTask>) = layers.addAll(elements.map(ctx::deduplicate))
    fun build() = LayerList(layers.toList(), background)
}