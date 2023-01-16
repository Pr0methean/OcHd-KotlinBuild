package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.scene.paint.Paint

abstract class SingleLayerMaterial(
    override val name: String,
    override val directory: String,
    private val sourceFileName: String,
    val color: Paint? = null,
    private val alpha: Double = 1.0
) : SingleTextureMaterial {
    override fun LayerListBuilder.createTextureLayers() {
        layer(sourceFileName, color, alpha)
    }

    override fun copyTo(dest: LayerListBuilder) {
        dest.layer(sourceFileName, color, alpha)
    }

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequenceOf(ctx.out(
            ctx.layer(sourceFileName, color, alpha), "$directory/$name"))
}
