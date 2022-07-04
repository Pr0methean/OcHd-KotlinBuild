package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Paint

interface ISingleLayerMaterial : SingleTextureMaterial {
    override val directory: String
    val sourceFileName: String
    val nameOverride: String?
        get() = null
    val color: Paint?
        get() = null
    val alpha: Double
        get() = 1.0
    override val name: String

    override fun LayerListBuilder.createTextureLayers()

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask>
}