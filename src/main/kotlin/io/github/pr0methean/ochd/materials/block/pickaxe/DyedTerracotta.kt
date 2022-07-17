package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object DyedTerracotta: Material {
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val baseTask = ctx.stack {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
            layer("borderRoundDots", TERRACOTTA.color)
        }
        DYES.forEach { (name, color) ->
            emit(ctx.out("block/${name}_terracotta", ctx.stack {
                background(color)
                copy(baseTask)
            }))
        }
    }
}