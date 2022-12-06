package io.github.pr0methean.ochd.materials.block.hoe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color.WHITE
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SimpleHoeBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SHROOMLIGHT(c(0xffac6d), c(0xd75100), c(0xffffb4)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("checksSmall", shadow)
            layer("shroomlightOn", highlight)
        }
    },
    TARGET_SIDE(c(0xffd7ba), Ore.REDSTONE.shadow, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("grassTall", highlight)
            layer("ringsCentralBullseye", shadow)
        }
    },
    TARGET_TOP(c(0xffd7ba), Ore.REDSTONE.shadow, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksSmall", highlight)
            layer("ringsCentralBullseye", shadow)
        }
    },
}
