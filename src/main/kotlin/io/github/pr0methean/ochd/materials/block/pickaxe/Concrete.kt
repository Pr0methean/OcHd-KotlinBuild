package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Concrete: DyedBlock("concrete") {
    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }

    override fun createSharedLayersTask(ctx: OutputTaskBuilder): AbstractImageTask = ctx.layer(ctx.stack {
        layer("strokeBottomLeftTopRight", c(0x515151))
        layer("strokeTopLeftBottomRight", c(0x515151))
        layer("borderShortDashes", c(0xaaaaaa))
    }, alpha = 0.25)
}
