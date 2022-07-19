package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

@Suppress("unused")
enum class GiantMushroom(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    ) : SingleTextureMaterial, ShadowHighlightMaterial, Block {
    RED_MUSHROOM_BLOCK(Color.RED, Color.BLACK, Color.WHITE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("mushroomSpots", highlight)
            layer("borderRoundDots", highlight)
        }
    },
    BROWN_MUSHROOM_BLOCK(c(0x977251), c(0x8d6850), c(0x9c795a)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("rings", highlight)
        }
    },
    MUSHROOM_STEM(c(0xc7c1b4), c(0xc2bcac), c(0xd3ccc4)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("stripesThick", shadow)
            layer("borderShortDashes", color)
        }
    },
    MUSHROOM_BLOCK_INSIDE(c(0xc7a877), c(0xab9066), c(0xd7b680)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("dots0", shadow)
            layer("borderRoundDotsVaryingSize", shadow)
        }
    }
}