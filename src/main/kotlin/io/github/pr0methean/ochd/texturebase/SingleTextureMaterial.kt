package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface SingleTextureMaterial: Material {
    val directory: String

    val name: String

    val nameOverride: String?
        get() = null

    suspend fun LayerListBuilder.createTextureLayers()

    suspend fun copyTo(dest: LayerListBuilder) {
        dest.copy(LayerListBuilder(dest.ctx).apply {createTextureLayers()}.build())
    }
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flowOf(
        ctx.out("$directory/${nameOverride ?: name}", ctx.stack { createTextureLayers() })
    )
}