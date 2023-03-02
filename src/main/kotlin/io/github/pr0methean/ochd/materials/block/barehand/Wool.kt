package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Wool : DyedBlock("wool") {
    override fun createSharedLayersTask(ctx: OutputTaskBuilder): AbstractImageTask = ctx.layer(ctx.stack {
        layer("zigzagBroken")
        layer("borderSolid")
        layer("zigzagBroken2", Color.WHITE)
        layer("borderDotted", Color.WHITE)
    }, alpha = 0.25)

    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }
}
