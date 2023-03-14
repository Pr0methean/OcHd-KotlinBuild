package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood.OAK
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.GOLD
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.IRON
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.texturebase.redstoneOffAndOn

object Rails: Material {
    override fun OutputTaskEmitter.outputTasks() {
        out("block/rail") {
            layer("railTies", OAK.color)
            layer("rail", IRON.refinedShadow)
        }
        out("block/rail_corner") {
            layer("railTieCorner", OAK.color)
            layer("railCorner", IRON.refinedShadow)
        }
        redstoneOffAndOn("block/powered_rail") { stateColor ->
            layer("railTies", OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", GOLD.color)
        }
        redstoneOffAndOn("block/activator_rail") { stateColor ->
            layer("railTies", OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", IRON.refinedShadow)
        }
        redstoneOffAndOn("block/detector_rail") { stateColor ->
            layer("railTies", OAK.shadow)
            layer("railDetectorPlate", stateColor)
            layer("rail", IRON.refinedShadow)
        }
    }
}
