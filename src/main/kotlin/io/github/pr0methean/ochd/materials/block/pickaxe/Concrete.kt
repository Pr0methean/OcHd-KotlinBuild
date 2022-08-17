package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Concrete: DyedBlock("concrete") {
    override suspend fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy(sharedLayersTask(ctx))
    }

    private suspend fun sharedLayersTask(ctx: ImageProcessingContext): ImageTask = ctx.stack {
        layer("x", DYES["gray"], 0.25)
        layer("borderLongDashes", DYES["light_gray"], 0.25)
    }
}