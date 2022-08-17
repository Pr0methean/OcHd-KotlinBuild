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
    }
}