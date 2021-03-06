package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Wool : DyedBlock("wool") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        layer("zigzagBroken", DYES["gray"], 0.25)
        layer("zigzagBroken2", DYES["light_gray"], 0.25)
        layer("borderSolid", DYES["gray"], 0.5)
        layer("borderDotted", DYES["light_gray"], 0.5)
    }
}