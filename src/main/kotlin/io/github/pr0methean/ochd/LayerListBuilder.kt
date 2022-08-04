package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.ConsumableImageTask
import io.github.pr0methean.ochd.tasks.consumable.ConsumableTask
import io.github.pr0methean.ochd.tasks.consumable.TopPartCroppingTask
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

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

    suspend fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    suspend fun layer(source: ConsumableTask<Image>, paint: Paint? = null, alpha: Double = 1.0): ConsumableImageTask {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    suspend fun layer(source: ConsumableImageTask, paint: Paint? = null, alpha: Double = 1.0) = layer(source.unpacked, paint, alpha)

    suspend fun copy(sourceInit: suspend LayerListBuilder.() -> Unit) =
        copy(LayerListBuilder(ctx).also {sourceInit()}.build())
    suspend fun copy(source: LayerList) {
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
            copy(ctx.stack {
                background(source.background)
                addAll(source.layers)
            })
        } else {
            addAll(source.layers)
        }
    }
    suspend fun copy(source: SingleTextureMaterial)
            = copy(LayerListBuilder(ctx).also {source.run {createTextureLayers()}}.build())
    suspend fun copyTopOf(source: ConsumableTask<Image>) = copy(TopPartCroppingTask(source, "Top part of $source", noopTaskCache(), ctx.stats))
    suspend fun copyTopOf(source: ConsumableImageTask) = copyTopOf(source.unpacked)
    suspend fun copyTopOf(source: suspend LayerListBuilder.() -> Unit) = copyTopOf(ctx.stack(source))
    suspend fun copy(element: ConsumableImageTask) = layers.add(ctx.deduplicate(element))
    suspend fun addAll(elements: Collection<ConsumableImageTask>) = layers.addAll(elements.asFlow().map(ctx::deduplicate).toList())
    fun build() = LayerList(layers.toList(), background)
}