package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Furnace: Material {
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val furnaceSide = ctx.stack {
            background(OreBase.STONE.color)
            layer("bottomHalf", OreBase.STONE.highlight)
            layer("borderSolid", OreBase.stoneExtremeShadow)
        }
        emit(ctx.out(furnaceSide, "block/furnace_side"))
        emit(ctx.out({
            copy(furnaceSide)
            layer("furnaceFront", Color.BLACK)
        }, "block/furnace_front"))
        emit(ctx.out({
            copy(furnaceSide)
            layer("furnaceFrontLit")
        }, "block/furnace_front_on"))
        val blastFurnaceTop = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        emit(ctx.out(blastFurnaceTop, "block/blast_furnace_top"))
        val blastFurnaceSide = ctx.stack {
            background(OreBase.STONE.shadow)
            layer("bottomHalf", OreBase.STONE.color)
            layer("cornerCrosshairs", OreBase.stoneExtremeHighlight)
        }
        emit(ctx.out(blastFurnaceSide, "block/blast_furnace"))
        val blastFurnaceFrontBase = ctx.stack {
            copy(blastFurnaceSide)
            layer("commandBlockSquare", OreBase.stoneExtremeHighlight)
        }
        emit(ctx.out({
            copy(blastFurnaceFrontBase)
            layer("blastFurnaceHoles")
        }, "block/blast_furnace_front"))
        emit(ctx.out(ctx.animate(listOf(
            ctx.stack {
                copy(blastFurnaceFrontBase)
                layer("blastFurnaceHolesLit")
            },
            ctx.stack {
                copy(blastFurnaceFrontBase)
                layer("blastFurnaceHolesLit1")
            }
        )), "block/blast_furnace_front_on"))
    }
}