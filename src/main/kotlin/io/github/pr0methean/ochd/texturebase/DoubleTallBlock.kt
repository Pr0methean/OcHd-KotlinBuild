package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DoubleTallBlock: Material {
    suspend fun LayerListBuilder.createBottomLayers()

    suspend fun LayerListBuilder.createTopLayers()

    val name: String

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        emit(ctx.out(ctx.stack { createBottomLayers() }, "block/${name}_bottom"))
        emit(ctx.out(ctx.stack { createTopLayers() }, "block/${name}_top"))
    }
}