package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood.OAK
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.REDSTONE
import io.github.pr0methean.ochd.texturebase.Material

object Torch: Material {
    override fun OutputTaskEmitter.outputTasks() {
        val torchBase = stack {
            layer("torchBase", OAK.highlight)
            layer("torchShadow", OAK.shadow)
        }
        out("block/torch") {
            copy(torchBase)
            layer("torchFlameSmall")
        }
        out("block/soul_torch") {
            copy(torchBase)
            layer("soulTorchFlameSmall")
        }
        out("block/redstone_torch_off") {
            copy(torchBase)
            layer("torchRedstoneHead")
        }
        out("block/redstone_torch") {
            copy(torchBase)
            layer("torchRedstoneHead", REDSTONE.highlight)
            layer("torchRedstoneHeadShadow", REDSTONE.shadow)
        }
    }
}
