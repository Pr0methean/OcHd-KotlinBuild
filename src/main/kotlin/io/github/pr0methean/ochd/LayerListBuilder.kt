package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.tasks.TextureTask
import io.github.pr0methean.ochd.tasks.TopPartCroppingTask
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.Deferred

@Suppress("DeferredIsResult")
class LayerListBuilder(val ctx: ImageProcessingContext) {
    private val layers = mutableListOf<Deferred<PackedImage>>()
    var background: Paint = Color.TRANSPARENT
    fun background(paint: Paint) {
        background = paint
    }

    fun background(color: Color, opacity: Double = 1.0) {
        background = if (opacity == 1.0) color else Color(color.red, color.green, color.blue, opacity * color.opacity)
    }
    fun background(red: Int, green: Int, blue: Int) {
        background(Color.rgb(red, green, blue))
    }
    fun background(color: Int) {
        background(c(color))
    }
    fun copy(element: Deferred<PackedImage>) = layers.add(element)

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> {
        val layer = ctx.layer(name, paint, alpha)
        copy(layer)
        return layer
    }

    fun layer(source: TextureTask, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> {
        val layer = ctx.layer(source, paint, alpha)
        copy(layer)
        return layer
    }

    fun copy(sourceInit: LayerListBuilder.() -> Unit) = copy(LayerListBuilder(ctx).also{sourceInit()}.build())
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
            layers.add(ctx.stack(layers))
        } else {
            layers.addAll(source.layers)
        }
    }
    fun copy(source: SingleTextureMaterial) {
        copy(ctx.stack { source.run { createTextureLayers() } })
    }
    fun copyTopOf(source: Deferred<PackedImage>) = copy(ctx.dedup(
        TopPartCroppingTask(source, ctx.tileSize, ctx.packer, ctx.scope, ctx.stats, ctx.retryer)))
    fun copyTopOf(source: LayerListBuilder.() -> Unit) = copyTopOf(ctx.stack(source))
    fun build(): LayerList {
        return LayerList(layers.toList(), background)
    }

    fun layer(task: Deferred<PackedImage>, paint: Paint? = null, alpha: Double = 1.0): Deferred<PackedImage> {
        val layer = ctx.layer(task, paint, alpha)
        copy(layer)
        return layer
    }
}