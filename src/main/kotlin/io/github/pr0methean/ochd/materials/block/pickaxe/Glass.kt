package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object Glass: Material {
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out(ctx.layer("paneTop", c(0xa8d5d5)), "block/glass_pane_top"))
        emit(ctx.out(ctx.stack {
            layer("borderSolid", DYES["gray"])
            layer("borderSolidTopLeft", Color.WHITE)
            layer("streaks", Color.WHITE)
        }, "block/glass"))
        emit(ctx.out(ctx.stack {
            background(Color.BLACK,0.25)
            layer("borderSolid", Color.WHITE, 0.25)
            layer("streaks", Color.WHITE, 0.25)
        }, "block/tinted_glass"))
    }
}