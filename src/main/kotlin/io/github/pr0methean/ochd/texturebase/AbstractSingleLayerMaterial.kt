package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Paint

abstract class AbstractSingleLayerMaterial(
    override val directory: String,
    override val sourceFileName: String,
    override val nameOverride: String?,
    override val color: Paint? = null,
    override val alpha: Double = 1.0
) : ISingleLayerMaterial {

    override val name: String
        get() = nameOverride ?: this::class.simpleName!!

    override fun LayerListBuilder.createTextureLayers() {
        layer(sourceFileName, color, alpha)
    }

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> {
        return listOf(ctx.out(name, ctx.layer(sourceFileName, color, alpha)))
    }
}