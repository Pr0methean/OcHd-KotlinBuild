package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material

object ConcretePowder: Material {

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> {
        val sharedLayersTask = ctx.stack {
            layer("checksSmall", DYES["gray"], 0.5)
            layer("checksSmall", DYES["light_gray"], 0.5)
        }
        return DYES.map {
            val name = it.key
            val color = it.value
            return@map ctx.out("block/${name}_concrete_powder", ctx.stack {
                background(color)
                add(sharedLayersTask)
            })
        }
    }

}