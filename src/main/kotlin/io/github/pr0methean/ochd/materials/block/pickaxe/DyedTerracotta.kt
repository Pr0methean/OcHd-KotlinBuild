package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import io.github.pr0methean.ochd.times
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
        copy {
            layer("bigDotsBottomLeftTopRight", TERRACOTTA.shadow * 0.5)
            layer("bigDotsTopLeftBottomRight", TERRACOTTA.highlight * 0.5)
        }
        copy {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
        }
        layer("borderRoundDots", TERRACOTTA.color)
    }
}
