package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object DyedTerracotta : DyedBlock("terracotta") {

    override suspend fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }

    override suspend fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.stack {
        layer("bigDotsTopLeftBottomRight", TERRACOTTA.shadow, 0.5)
        layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
        layer("bigDotsBottomLeftTopRight", TERRACOTTA.highlight, 0.5)
        layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
        layer("borderRoundDots", TERRACOTTA.color)
    }
}
