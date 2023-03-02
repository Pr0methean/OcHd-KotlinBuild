package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
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

    override fun createSharedLayersTask(ctx: OutputTaskBuilder): AbstractImageTask = ctx.stack {
        layer(ctx.stack {
            layer("bigDotsBottomLeftTopRight", TERRACOTTA.shadow)
            layer("bigDotsTopLeftBottomRight", TERRACOTTA.highlight)
        }, alpha = 0.5)
        copy {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
        }
        layer("borderRoundDots", TERRACOTTA.color)
    }
}
