package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class SingleLayerMaterial(
    override val directory: String,
    open val sourceFileName: String,
    open val nameOverride: String?,
    open val color: Paint? = null,
    open val alpha: Double = 1.0
) : SingleTextureMaterial {

    override val name: String
        get() = nameOverride ?: this::class.simpleName!!

    override fun LayerListBuilder.createTextureLayers() {
        layer(sourceFileName, color, alpha)
    }

    override fun rawOutputTasks(ctx: ImageProcessingContext): Flow<OutputTask> {
        return ctx.decorateFlow(flowOf(ctx.out(name, ctx.layer(sourceFileName, color, alpha))))
    }

}