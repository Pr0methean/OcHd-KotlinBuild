package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object StainedGlassFront: DyedBlock("stained_glass") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        background(Color(color.red, color.green, color.blue, 0.25))
        layer("borderSolid", color)
        layer("streaks", color)
    }
}
