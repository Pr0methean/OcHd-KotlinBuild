package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.axe.OverworldWood
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.texturebase.redstoneOffAndOn

object Rails: Material {
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out({
            layer("railTies", OverworldWood.OAK.color)
            layer("rail", Ore.IRON.refinedShadow)
        }, "block/rail"))
        yield(ctx.out({
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
