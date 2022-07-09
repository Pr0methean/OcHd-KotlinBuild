package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map

object Concrete: Material {
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = DYES.entries.asFlow().map {
        val name = it.key
        val color = it.value
        ctx.out("block/${name}_concrete", ctx.stack {
            background(color)
            layer("x", DYES["gray"], 0.25)
            layer("borderLongDashes", DYES["light_gray"], 0.25)
        })
    }
}