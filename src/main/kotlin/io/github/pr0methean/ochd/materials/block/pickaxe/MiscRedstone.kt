package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.REDSTONE
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.SMOOTH_STONE
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.texturebase.redstoneOffAndOn

object MiscRedstone: Material {
    override fun OutputTaskBuilder.outputTasks() {
        val repeaterComparatorCommonBase = stack {
            copy(SMOOTH_STONE)
            layer("repeaterSideInputs", STONE.shadow)
        }
        redstoneOffAndOn("block/repeater") { stateColor ->
            copy(repeaterComparatorCommonBase)
            layer("repeater", stateColor)
        }
        redstoneOffAndOn("block/comparator") { stateColor ->
            copy(repeaterComparatorCommonBase)
            layer("comparator", stateColor)
        }
        out("block/redstone_lamp") {
            background(REDSTONE.shadow)
            layer("lamp", REDSTONE.highlight)
            layer("borderSolid")
            layer("borderSolidTopLeft", REDSTONE.highlight)
        }
        out("block/redstone_lamp_on") {
            val color = c(0xe6994a)
            val shadow = c(0x946931)
            val highlight = c(0xFFCDB2)
            background(color)
            layer("lampOn", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        }
    }
}
