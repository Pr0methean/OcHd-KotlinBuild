package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object BoneBlock: Material {
    val color = c(0xe1ddca)
    val shadow = c(0xc3bfa1)
    val highlight = c(0xEaEaD0)
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        emit(ctx.out({
            background(shadow)
            layer("borderSolid", highlight)
            layer("bonesXor", highlight)
        }, "block/bone_block_top"))
        emit(ctx.out({
            background(color)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
            layer("boneBottomLeftTopRight", shadow)
            layer("boneTopLeftBottomRight", highlight)
        }, "block/bone_block_side"))
    }
}