package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.Material

object Torch: Material {
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val torchBase = ctx.stack {
            layer("torchBase", OverworldWood.OAK.highlight)
            layer("torchShadow", OverworldWood.OAK.shadow)
        }
        yield(ctx.out("block/torch") {
            copy(torchBase)
            layer("torchFlameSmall")
        })
        yield(ctx.out("block/soul_torch") {
            copy(torchBase)
            layer("soulTorchFlameSmall")
        })
        yield(ctx.out("block/redstone_torch_off") {
            copy(torchBase)
            layer("torchRedstoneHead")
        })
        yield(ctx.out("block/redstone_torch") {
            copy(torchBase)
            layer("torchRedstoneHead", Ore.REDSTONE.highlight)
            layer("torchRedstoneHeadShadow", Ore.REDSTONE.shadow)
        })
    }
}
