package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.OreBase
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SimpleSoftEarth(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    override val hasOutput: Boolean = true
) : ShadowHighlightMaterial, Block {
    SAND(c(0xdfd5aa), c(0xd1ba8a), c(0xEaEaD0)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("checksSmall", shadow)
            layer("checksSmallOutline", highlight)
        }
    },
    GRAVEL(c(0x737373), c(0x515151), c(0xaaaaaa)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksLarge", shadow)
            layer("checksLargeOutline", highlight)
        }
    },
    RED_SAND(c(0xbf6721), c(0xac5700), c(0xd97b30)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", highlight)
            layer("checksSmall", highlight)
            layer("checksSmallOutline", shadow)
        }
    },
    CLAY(c(0x9aa3b3), c(0x9494a4), c(0xA8BEC5)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", highlight)
            layer("diagonalChecksBottomLeftTopRight", shadow)
            layer("diagonalChecksFillTopLeftBottomRight", shadow)
            layer("diagonalChecksFillBottomLeftTopRight", highlight)
        }
    },
    MUD(c(0x3a3a3a), c(0x333333), c(0x515151)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("borderSolid", highlight)
            layer("borderDotted", shadow)
        }
    },
    MOSS_BLOCK(c(0x647233),c(0x42552d),c(0x70922d)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight4", highlight)
            layer("strokeBottomLeftTopRight4", shadow)
            layer("borderSolid", highlight)
            layer("borderShortDashes", shadow)
        }
    },
    SOUL_SAND(c(0x624033), c(0x3F2D23), c(0x915431)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("checksSmall", highlight)
            layer("bigDotsTopLeftBottomRight", shadow)
            layer("soulFaces", highlight)
        }
    },
    SOUL_SOIL(c(0x3F2D23), c(0x352922), c(0x915431)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(shadow)
            layer("borderSolid", color)
            layer("strokeBottomLeftTopRight4", highlight)
            layer("bigDotsTopLeftBottomRight", highlight)
            layer("soulFaces", shadow)
        }
    },
    PACKED_MUD(c(0x8c674f),c(0x5e4841),c(0xab8661)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("borderDotted", MUD.highlight)
        }
    },
    FARMLAND(c(0x966c4a),c(0x593d29),c(0xb9855c)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("bambooThick", color)
            layer("bambooThinMinusBorder", shadow)
        }
    },
    FARMLAND_MOIST(c(0x552e00),c(0x341900),c(0x6e3c15)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(highlight)
            layer("bambooThick", color)
            layer("bambooThinMinusBorder", shadow)
            layer("dots0", OreBase.STONE.shadow)
        }
    },
    DIRT(c(0x966c4a), c(0x593d29), c(0xb9855c)) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("dots3", shadow)
            layer("dots2", highlight)
            layer("borderDotted", highlight)
        }
    },
    POWDER_SNOW(Color.WHITE,  c(0xcfcfdf), Color.WHITE) {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("snowXorChecksSmall", shadow)
        }
    };
    constructor(base: ShadowHighlightMaterial):
            this(base.color, base.shadow, base.highlight)
}
