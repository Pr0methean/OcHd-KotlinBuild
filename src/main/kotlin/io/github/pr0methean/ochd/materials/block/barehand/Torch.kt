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
        yield(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchFlameSmall")
        }, "block/torch"))
        yield(ctx.out(ctx.stack {
            copy(torchBase)
            layer("soulTorchFlameSmall")
        }, "block/soul_torch"))
        yield(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead")
        }, "block/redstone_torch_off"))
        yield(ctx.out(ctx.stack {
            copy(torchBase)
            layer("torchRedstoneHead", Ore.REDSTONE.highlight)
            layer("torchRedstoneHeadShadow", Ore.REDSTONE.shadow)
        }, "block/redstone_torch"))
    }
}
