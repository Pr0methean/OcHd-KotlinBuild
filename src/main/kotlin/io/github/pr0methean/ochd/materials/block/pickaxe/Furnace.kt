package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Furnace: Material {
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val furnaceSide = ctx.stack {
            background(OreBase.STONE.color)
            layer("bottomHalf", OreBase.STONE.highlight)
            layer("borderSolid", OreBase.stoneExtremeShadow)
        }
        emit(ctx.out("block/furnace_side", furnaceSide))
        emit(ctx.out("block/furnace_front") {
            copy(furnaceSide)
            layer("furnaceFront", Color.BLACK)
        })
        emit(ctx.out("block/furnace_front_on") {
            copy(furnaceSide)
            layer("furnaceFrontLit")
        })
    }
}