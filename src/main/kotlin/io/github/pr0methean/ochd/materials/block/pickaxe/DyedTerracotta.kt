package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object DyedTerracotta: DyedBlock("terracotta") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.shadow)
            layer("borderRoundDots", TERRACOTTA.color)
        }
    }
}