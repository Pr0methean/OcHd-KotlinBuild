package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.tasks.consumable.RepaintTask
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object StainedGlassFront: Material {

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val masterTask = ctx.stack {
            background(Color.BLACK, 0.25)
            layer("borderSolid", Color.BLACK)
            layer("streaks", Color.BLACK)
        }
        masterTask.enableCaching()
        DYES.forEach { (name, color) ->
            emit(ctx.out("block/${name}_stained_glass", RepaintTask(masterTask, color, 1.0, noopTaskCache(), ctx.stats)))
        }
    }
}
