package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class DyedBlock(val name: String): Material {
    abstract suspend fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    )

    abstract suspend fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<PngOutputTask> = flow {
        val sharedLayersTask = createSharedLayersTask(ctx)
        DYES.forEach { (dyeName, color) ->
            emit(ctx.out(ctx.stack {createTextureLayers(color, sharedLayersTask)}, "block/${dyeName}_$name"))
        }
    }
}
