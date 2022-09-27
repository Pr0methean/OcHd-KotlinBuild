package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String
    val nameOverrideTop: String?
            get() = null
    val nameOverrideSide: String?
            get() = null

    suspend fun LayerListBuilder.createCoverSideLayers()
    suspend fun LayerListBuilder.createTopLayers()

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        emit(ctx.out(ctx.stack { createTopLayers() }, "block/${nameOverrideTop ?: "${name}_top"}"))
        emit(ctx.out(ctx.stack {
            copy(base)
            createCoverSideLayers()
        }, "block/${nameOverrideSide ?: "${name}_side"}"))
    }
}
