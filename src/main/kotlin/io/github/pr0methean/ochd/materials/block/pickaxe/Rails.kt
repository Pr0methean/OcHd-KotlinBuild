package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.texturebase.redstoneOffAndOn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Rails: Material {
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out({
            layer("railTies", OverworldWood.OAK.color)
            layer("rail", Ore.IRON.refinedShadow)
        }, "block/rail"))
        emit(ctx.out({
            layer("railTieCorner", OverworldWood.OAK.color)
            layer("railCorner", Ore.IRON.refinedShadow)
        }, "block/rail_corner"))
        redstoneOffAndOn(ctx, "block/powered_rail") { stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", Ore.GOLD.color)
        }
        redstoneOffAndOn(ctx, "block/activator_rail") { stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("thirdRail", stateColor)
            layer("rail", Ore.IRON.refinedShadow)
        }
        redstoneOffAndOn(ctx, "block/detector_rail") {stateColor ->
            layer("railTies", OverworldWood.OAK.shadow)
            layer("railDetectorPlate", stateColor)
            layer("rail", Ore.IRON.refinedShadow)
        }
    }
}