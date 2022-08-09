package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.consumable.ImageTask
import io.github.pr0methean.ochd.tasks.consumable.Task
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class LayerListBuilder(val ctx: ImageProcessingContext) {
    private val layers = mutableListOf<ImageTask>()
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

    suspend fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    suspend fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    suspend fun layer(source: ImageTask, paint: Paint? = null, alpha: Double = 1.0) = layer(source.unpacked, paint, alpha)

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
    suspend fun copy(source: SingleTextureMaterial): Unit = copy(LayerListBuilder(ctx).also {source.run {createTextureLayers()}}.build())

    fun copy(element: ImageTask): Boolean = layers.add(element)
    private fun addAll(elements: Collection<ImageTask>) = layers.addAll(elements)
    suspend fun build() = LayerList(layers.asFlow().map(ctx::deduplicate).toList(), background)
}