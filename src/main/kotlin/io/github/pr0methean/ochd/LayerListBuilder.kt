package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

@OcHdDslMarker
class LayerListBuilder(val ctx: TaskPlanningContext) {
    private val layers: MutableList<AbstractImageTask> = mutableListOf()
    var background: Paint = Color.TRANSPARENT

    fun background(paint: Paint) {
        check(layers.isEmpty()) { "Background would overwrite layers: $layers" }
        check(background == Color.TRANSPARENT) { "Background would overwrite another background: $background " }
        background = paint
    }

    fun background(red: Int, green: Int, blue: Int) {
        background = Color.rgb(red, green, blue)
    }

    fun background(color: Int) {
        background = c(color)
    }

    fun layer(name: String, paint: Paint? = null, alpha: Double = 1.0) {
        layers.add(ctx.layer(ctx.findSvgTask(name), paint, alpha))
    }

    fun layer(
        source: AbstractImageTask, paint: Paint? = null, alpha: Double = 1.0
    ) {
        layers.add(ctx.layer(source, paint, alpha))
    }

    inline fun copy(sourceInit: LayerListBuilder.() -> Unit): Unit =
        copy(LayerListBuilder(ctx).apply(sourceInit).build())

    fun copy(source: LayerList) {
        check(source.layers.isNotEmpty()) { "Copying from empty LayerList" }
        if (source.background != Color.TRANSPARENT) {
            background(source.background) // For validation, even if we don't end up using it
        }
        if (source.layers.size > 1) { // Don't flatten sub-stacks since we want to deduplicate them at build time
            layers.add(ctx.stack(source))
            if (source.background != Color.TRANSPARENT) {
                background = Color.TRANSPARENT // let source draw the background
            }
        } else {
            layers.add(source.layers[0])
        }
    }

    fun copy(source: SingleTextureMaterial): Unit = source.copyTo(this)

    @Suppress("ComplexCondition")
    fun copy(element: AbstractImageTask) {
        val currentTop = layers.lastOrNull()
        if (currentTop != null) {
            layers.removeLast()
            layers.addAll(ctx.deduplicate<Image,AbstractImageTask>(element)
                .tryCombineWith(currentTop, ctx)
                .map(ctx::deduplicate))
        } else {
            layers.add(element)
        }
    }

    fun build(): LayerList {
        check(layers.isNotEmpty()) { "Trying to create an empty LayerList" }
        return LayerList(layers, background, layers.maxOf { it.width }, layers.maxOf { it.height })
    }
}
