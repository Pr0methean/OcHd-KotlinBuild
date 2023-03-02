package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase.STONE
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object Furnace: Material {
    override suspend fun OutputTaskBuilder.outputTasks() {
        val furnaceSide = stack {
            background(STONE.color)
            layer("bottomHalf", STONE.highlight)
            layer("borderSolid", OreBase.stoneExtremeShadow)
        }
        out("block/furnace_side", furnaceSide)
        out("block/furnace_front") {
            copy(furnaceSide)
            layer("furnaceFrontLit", Color.BLACK)
        }
        out("block/furnace_front_on") {
            copy(furnaceSide)
            layer("furnaceFrontLit")
        }
        val blastFurnaceTop = stack {
            background(STONE.shadow)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        out("block/blast_furnace_top", blastFurnaceTop)
        val blastFurnaceSide = stack {
            background(STONE.shadow)
            layer("bottomHalf", STONE.color)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        out("block/blast_furnace", blastFurnaceSide)
        val blastFurnaceFrontBase = stack {
            copy(blastFurnaceSide)
            layer("craftingGridSquare", OreBase.stoneExtremeHighlight)
        }
        out("block/blast_furnace_front") {
            copy(blastFurnaceFrontBase)
            layer("blastFurnaceHoles")
        }
        out("block/blast_furnace_front_on",
            animate(blastFurnaceFrontBase, listOf(
                layer("blastFurnaceHolesLit"),
                layer("blastFurnaceHolesLit1")
            ))
        )
    }
}
