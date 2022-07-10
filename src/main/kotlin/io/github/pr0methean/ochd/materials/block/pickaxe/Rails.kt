package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.redstoneOffAndOn
import io.github.pr0methean.ochd.texturebase.Material
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Rails: Material {
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/rail") {
            layer("railTies", OverworldWood.OAK.color)
            layer("rail", OreBase.STONE.highlight)
        })
        emit(ctx.out("block/rail_corner") {
            layer("railTieCorner", OverworldWood.OAK.color)
            layer("railCorner", OreBase.STONE.highlight)
        })
        redstoneOffAndOn(ctx, "block/powered_rail") { stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", Ore.GOLD.color)
        }
        redstoneOffAndOn(ctx, "block/activator_rail") { stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", OreBase.STONE.highlight)
        }
        redstoneOffAndOn(ctx, "block/detector_rail") {stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("railDetectorPlate", stateColor)
            layer("rail", OreBase.STONE.highlight)
        }
    }
}