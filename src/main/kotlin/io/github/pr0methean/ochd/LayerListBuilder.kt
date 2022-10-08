package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.Task
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList

class LayerListBuilder(val ctx: TaskPlanningContext) {
    val layers: MutableList<ImageTask> = mutableListOf()
    var background: Paint = Color.TRANSPARENT
    fun background(paint: Paint, opacity: Double = 1.0) {
        background = if (opacity == 1.0 || paint !is Color) paint else Color(paint.red, paint.green, paint.blue, opacity * paint.opacity)
    }
    fun background(red: Int, green: Int, blue: Int) {
        background = Color.rgb(red, green, blue)
    }
    fun background(color: Int) {
        background = c(color)
    }

    suspend inline fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    suspend inline fun layer(source: Task<Image>, paint: Paint? = null, alpha: Double = 1.0): ImageTask {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    suspend inline fun copy(sourceInit: LayerListBuilder.() -> Unit): Unit =
        copy(LayerListBuilder(ctx).also {sourceInit()}.build())
    suspend inline fun copy(source: LayerList) {
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
            copy(ctx.stack(source))
        } else {
            addAll(source.layers)
        }
    }
    suspend fun copy(source: SingleTextureMaterial): Unit = copy(LayerListBuilder(ctx).also {source.run {createTextureLayers()}}.build())

    suspend fun copy(element: ImageTask): Boolean {
        val deduped = ctx.deduplicate(element)
        return layers.add(deduped)
    }
    fun addAll(elements: Collection<ImageTask>): Boolean = layers.addAll(elements)
    suspend fun build(): LayerList = LayerList(layers.asFlow().map(ctx::deduplicate).toList(), background)
}