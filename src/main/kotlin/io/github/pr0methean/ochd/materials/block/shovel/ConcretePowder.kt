package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.consumable.ConsumableImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object ConcretePowder: DyedBlock("concrete_powder") {
    override suspend fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy(sharedLayersTask(ctx))
    }

    private suspend fun sharedLayersTask(ctx: ImageProcessingContext): ConsumableImageTask = ctx.stack {
        layer("checksSmall", DYES["gray"], 0.5)
        layer("checksSmall", DYES["light_gray"], 0.5)
    }
}