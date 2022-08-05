package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.texturebase.redstoneOffAndOn
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object MiscRedstone: Material {
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val repeaterComparatorCommonBase = ctx.stack {
            copy(SimplePickaxeBlock.SMOOTH_STONE)
            layer("repeaterSideInputs", OreBase.STONE.shadow)
        }
        redstoneOffAndOn(ctx, "block/repeater") { stateColor ->
            copy(repeaterComparatorCommonBase)
            layer("repeater", stateColor)
        }
        redstoneOffAndOn(ctx, "block/comparator") {stateColor ->
            copy(repeaterComparatorCommonBase)
            layer("comparator", stateColor)
        }
        emit(ctx.out("block/redstone_lamp") {
            background(Ore.REDSTONE.shadow)
            layer("lamp", Ore.REDSTONE.highlight)
            layer("borderSolid", Color.BLACK)
            layer("borderSolidTopLeft", Ore.REDSTONE.highlight)
        })
        emit(ctx.out("block/redstone_lamp_on") {
            val color = c(0xe6994a)
            val shadow = c(0x946931)
            val highlight = c(0xffdab4)
            background(color)
            layer("lampOn", highlight)
            layer("borderSolid", shadow)
            layer("borderSolidTopLeft", highlight)
        })
    }
}