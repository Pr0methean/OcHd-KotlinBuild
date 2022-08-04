package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock.TERRACOTTA
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object DyedTerracotta: DyedBlock("terracotta") {
    override suspend fun LayerListBuilder.createTextureLayers(color: Color) {
        background(color)
        copy {
            layer("bigRingsTopLeftBottomRight", TERRACOTTA.highlight)
            layer("bigDotsBottomLeftTopRight", TERRACOTTA.shadow)
            layer("bigRingsBottomLeftTopRight", TERRACOTTA.highlight)
            layer("borderRoundDots", TERRACOTTA.color)
        }
    }
}