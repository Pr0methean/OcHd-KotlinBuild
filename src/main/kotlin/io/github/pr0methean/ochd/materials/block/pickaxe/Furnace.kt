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
        yield(ctx.out(furnaceSide, "block/furnace_side"))
        yield(ctx.out({
            copy(furnaceSide)
            layer("furnaceFront", Color.BLACK)
        }, "block/furnace_front"))
        yield(ctx.out({
            copy(furnaceSide)
            layer("furnaceFrontLit")
        }, "block/furnace_front_on"))
        val blastFurnaceTop = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out(blastFurnaceTop, "block/blast_furnace_top"))
        val blastFurnaceSide = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("bottomHalf", OreBase.STONE.color)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out(blastFurnaceSide, "block/blast_furnace"))
        val blastFurnaceFrontBase = ctx.stack {
            copy(blastFurnaceSide)
            layer("craftingGridSquare", OreBase.stoneExtremeHighlight)
        }
        yield(ctx.out({
            copy(blastFurnaceFrontBase)
            layer("blastFurnaceHoles")
        }, "block/blast_furnace_front"))
        yield(ctx.out(ctx.animate(blastFurnaceFrontBase, listOf(
            ctx.layer("blastFurnaceHolesLit"),
            ctx.layer("blastFurnaceHolesLit1")
        )), "block/blast_furnace_front_on"))
    }
}
