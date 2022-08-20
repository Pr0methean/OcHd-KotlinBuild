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
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("mushroomSpots", highlight)
            layer("borderRoundDots", highlight)
        }
    },
    BROWN_MUSHROOM_BLOCK(c(0x977251),  c(0x915431), c(0x9d825e)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("rings", highlight)
        }
    },
    MUSHROOM_STEM(c(0xc4c4b4), c(0xc0c0ac), c(0xd0d0c4)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("stripesThick", shadow)
            layer("borderShortDashes", color)
        }
    },
    MUSHROOM_BLOCK_INSIDE(c(0xc7a877), c(0xab9066), c(0xD7C187)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("dots0", shadow)
            layer("borderRoundDotsVaryingSize", shadow)
        }
    }
}