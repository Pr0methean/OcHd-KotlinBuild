package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.TextureTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object ConcretePowder: DyedBlock("concrete_powder") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy(sharedLayersTask(ctx))
    }

    private fun sharedLayersTask(ctx: ImageProcessingContext): TextureTask = ctx.stack {
        layer("checksSmall", DYES["gray"], 0.5)
        layer("checksSmall", DYES["light_gray"], 0.5)
    }
}