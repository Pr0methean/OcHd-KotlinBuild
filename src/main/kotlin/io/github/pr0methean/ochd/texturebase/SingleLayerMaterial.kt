package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

abstract class SingleLayerMaterial(
    override val name: String,
    override val directory: String,
    val sourceFileName: String,
    val color: Paint? = null,
    val alpha: Double = 1.0
) : SingleTextureMaterial {
    override suspend fun LayerListBuilder.createTextureLayers() {
        layer(sourceFileName, color, alpha)
    }

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> {
        return flowOf(ctx.out(
            ctx.layer(sourceFileName, color, alpha), "$directory/$name"))
    }

}