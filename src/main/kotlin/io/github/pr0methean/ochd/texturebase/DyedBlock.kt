package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class DyedBlock(val name: String): Material {
    abstract suspend fun LayerListBuilder.createTextureLayers(color: Color)

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        DYES.forEach {
            val dyeName = it.key
            val color = it.value
            emit(ctx.out(ctx.stack {createTextureLayers(color)}, "block/${dyeName}_$name"))
        }
    }
}