package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.Deferred

object ConcretePowder: DyedBlock("concrete_powder") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy(sharedLayersTask(ctx))
    }

    @Suppress("DeferredResultUnused", "DeferredIsResult")
    private fun sharedLayersTask(ctx: ImageProcessingContext): Deferred<PackedImage> = ctx.stack {
        layer("checksSmall", DYES["gray"], 0.5)
        layer("checksSmall", DYES["light_gray"], 0.5)
    }
}