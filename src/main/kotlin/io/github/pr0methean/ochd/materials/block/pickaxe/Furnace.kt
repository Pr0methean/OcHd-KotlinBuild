package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object Furnace: Material {
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val furnaceSide = ctx.stack {
            background(OreBase.STONE.color)
            layer("bottomHalf", OreBase.STONE.highlight)
            layer("borderSolid", OreBase.stoneExtremeShadow)
        }
        yield(ctx.out("block/furnace_side", furnaceSide))
        yield(ctx.out("block/furnace_front") {
            copy(furnaceSide)
            layer("furnaceFrontLit", Color.BLACK)
        })
        yield(ctx.out("block/furnace_front_on") {
            copy(furnaceSide)
            layer("furnaceFrontLit")
        })
        val blastFurnaceTop = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out("block/blast_furnace_top", blastFurnaceTop))
        val blastFurnaceSide = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("bottomHalf", OreBase.STONE.color)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out("block/blast_furnace", blastFurnaceSide))
        val blastFurnaceFrontBase = ctx.stack {
            copy(blastFurnaceSide)
            layer("craftingGridSquare", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out("block/blast_furnace_front") {
            copy(blastFurnaceFrontBase)
            layer("blastFurnaceHoles")
        })
        yield(ctx.out("block/blast_furnace_front_on",
            ctx.animate(blastFurnaceFrontBase, listOf(
                ctx.layer("blastFurnaceHolesLit"),
                ctx.layer("blastFurnaceHolesLit1")
            ))
        ))
    }
}
