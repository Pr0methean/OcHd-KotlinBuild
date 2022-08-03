package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Glass: Material {
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/glass_pane_top", ctx.layer("paneTop", c(0xa8d0d9))))
        emit(ctx.out("block/glass", ctx.stack {
            layer("borderSolid", Color.WHITE)
            layer("borderSolidBottomRight", DYES["gray"])
            layer("streaks", Color.WHITE)
        }))
        emit(ctx.out("block/tinted_glass", ctx.stack {
            background(Color.BLACK,0.25)
            layer("borderSolid", Color.WHITE, 0.25)
            layer("streaks", Color.WHITE, 0.25)
        }))
    }
}