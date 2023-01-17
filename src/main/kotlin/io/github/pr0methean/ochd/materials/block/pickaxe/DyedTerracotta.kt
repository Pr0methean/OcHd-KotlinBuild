package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object DyedTerracotta : DyedBlock("terracotta") {

    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }

    override fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.stack {
        layer(ctx.stack {
            layer("bigDotsBottomLeftTopRight", TERRACOTTA.highlight)
            layer("bigDotsTopLeftBottomRight", TERRACOTTA.shadow)
        }, alpha = 0.5)
        layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
        layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
        layer("borderRoundDots", TERRACOTTA.color)
    }
}
