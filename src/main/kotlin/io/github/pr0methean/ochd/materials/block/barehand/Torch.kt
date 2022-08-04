package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Torch: Material {
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        /*

push torchBase $wood_oak torch1
push torchShadow $wood_oak_s torch2
push_precolored torchFlameSmall torch3
out_stack block/torch

push torchBase $wood_oak storch1
push torchShadow $wood_oak_s storch2
push_precolored soulTorchFlameSmall storch3
out_stack block/soul_torch

push torchBase $wood_oak rtorch1
push torchShadow $wood_oak_s rtorch2
push torchRedstoneHead $black rtorch3
out_stack block/redstone_torch_off

push torchBase $wood_oak artorch1
push torchShadow $wood_oak_s artorch2
push torchRedstoneHead ${redstone_h} artorch3
push torchRedstoneHeadShadow ${redstone_s} artorch4
out_stack block/redstone_torch
         */
        val torchBase = ctx.stack {
            layer("torchBase", OverworldWood.OAK.highlight)
            layer("torchShadow", OverworldWood.OAK.shadow)
        }
        emit(ctx.out("block/torch", ctx.stack {
            copy(torchBase)
            layer("torchFlameSmall")
        }))
        emit(ctx.out("block/soul_torch", ctx.stack {
            copy(torchBase)
            layer("soulTorchFlameSmall")
        }))
        emit(ctx.out("block/redstone_torch_off", ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead")
        }))
        emit(ctx.out("block/redstone_torch", ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead", Ore.REDSTONE.highlight)
            layer("torchRedstoneHeadShadow", Ore.REDSTONE.shadow)
        }))
    }
}