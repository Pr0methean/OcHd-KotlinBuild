package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material

object ConcretePowder: Material {

    override fun outputTasks(ctx: ImageProcessingContext): Sequence<OutputTask> = sequence {
        val sharedLayersTask = ctx.stack {
            layer("checksSmall", DYES["gray"], 0.5)
            layer("checksSmall", DYES["light_gray"], 0.5)
        }
        DYES.forEach {
            val name = it.key
            val color = it.value
            yield(ctx.out("block/${name}_concrete_powder", ctx.stack {
                background(color)
                add(sharedLayersTask)
            }))
        }
    }

}