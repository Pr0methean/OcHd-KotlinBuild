package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow

object DyedTerracotta : DyedBlock("terracotta") {
    var masterTask: ImageTask? = null
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> {
        masterTask = ctx.stack {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigDotsBottomLeftTopRight", TERRACOTTA.shadow)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.highlight)
            layer("borderRoundDots", TERRACOTTA.color)
        }
        return super.outputTasks(ctx)
    }

    override suspend fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy(masterTask!!)
    }
}