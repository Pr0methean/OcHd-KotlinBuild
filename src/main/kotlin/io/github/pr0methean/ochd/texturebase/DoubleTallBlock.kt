package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface DoubleTallBlock: Material {
    fun LayerListBuilder.createBottomLayers()

    fun LayerListBuilder.createTopLayers()

    val name: String

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/${name}_bottom", ctx.stack { createBottomLayers() }))
        emit(ctx.out("block/${name}_top", ctx.stack { createTopLayers() }))
    }
}