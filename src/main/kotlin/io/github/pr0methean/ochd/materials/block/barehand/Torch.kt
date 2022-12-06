package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Torch: Material {
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        val torchBase = ctx.stack {
            layer("torchBase", OverworldWood.OAK.highlight)
            layer("torchShadow", OverworldWood.OAK.shadow)
        }
        emit(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchFlameSmall")
        }, "block/torch"))
        emit(ctx.out(ctx.stack {
            copy(torchBase)
            layer("soulTorchFlameSmall")
        }, "block/soul_torch"))
        emit(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead")
        }, "block/redstone_torch_off"))
        emit(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead", Ore.REDSTONE.highlight)
            layer("torchRedstoneHeadShadow", Ore.REDSTONE.shadow)
        }, "block/redstone_torch"))
    }
}
