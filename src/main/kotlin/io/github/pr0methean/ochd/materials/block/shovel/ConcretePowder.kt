package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object ConcretePowder: Material {

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val sharedLayersTask = ctx.stack {
            layer("checksSmall", DYES["gray"], 0.5)
            layer("checksSmall", DYES["light_gray"], 0.5)
        }
        DYES.forEach {
            val name = it.key
            val color = it.value
            emit(ctx.out("block/${name}_concrete_powder", ctx.stack {
                background(color)
                copy(sharedLayersTask)
            }))
        }
    }

}