package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

abstract class DyedBlock(val name: String): Material {
    abstract fun LayerListBuilder.createTextureLayers(color: Color)

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        DYES.forEach {
            val dyeName = it.key
            val color = it.value
            emit(ctx.out("block/${dyeName}_$name", ctx.stack {createTextureLayers(color)}))
        }
    }
}