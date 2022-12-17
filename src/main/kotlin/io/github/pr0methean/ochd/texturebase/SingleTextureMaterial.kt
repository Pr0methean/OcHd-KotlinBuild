package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SingleTextureMaterial: Material {
    val directory: String

    val name: String

    suspend fun LayerListBuilder.createTextureLayers()

    suspend fun copyTo(dest: LayerListBuilder) {
        dest.copy(LayerListBuilder(dest.ctx).apply {createTextureLayers()}.build())
    }
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<PngOutputTask> = flowOf(
        ctx.out(ctx.stack { createTextureLayers() }, "$directory/$name")
    )
}
