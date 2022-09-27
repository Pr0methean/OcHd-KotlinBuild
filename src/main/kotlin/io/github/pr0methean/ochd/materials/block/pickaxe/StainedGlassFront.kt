package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.tasks.RepaintTask
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * This is the only material that feeds an ImageStackingTask to a RepaintTask rather than vice-versa.
 */
object StainedGlassFront: Material {

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        val masterTask = ctx.stack {
            background(Color.BLACK, 0.25)
            layer("borderSolid", Color.BLACK)
            layer("streaks", Color.BLACK)
        }
        DYES.forEach { (name, color) ->
            emit(ctx.out(RepaintTask(masterTask, color, 1.0, noopTaskCache(), ctx.stats), "block/${name}_stained_glass"))
        }
    }
}
