package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object BoneBlock: Material {
    val color = c(0xe1ddca)
    val shadow = c(0xc3bfa1)
    val highlight = c(0xe9e6d4)
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/bone_block_top") {
            background(shadow)
            layer("borderSolid", highlight)
            layer("bonesXor", highlight)
        })
        emit(ctx.out("block/bone_block_side") {
            background(color)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
            layer("boneBottomLeftTopRight", shadow)
            layer("boneTopLeftBottomRight", highlight)
        })
    }
}