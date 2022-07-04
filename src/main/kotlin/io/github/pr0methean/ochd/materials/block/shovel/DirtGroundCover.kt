package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

val DIRT_GROUND_COVERS = group<DirtGroundCover>()
enum class DirtGroundCover(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    override val nameOverrideTop: String? = null,
    override val nameOverrideSide: String? = null
): ShadowHighlightMaterial, GroundCoverBlock {
    /**
     * Grass is a gray texture, modified by a colormap according to the biome.
     */
    GRASS(c(0x9d9d9d), c(0x828282), c(0xbababa), nameOverrideSide = "grass_block_side_overlay") {
        val extremeShadow = c(0x757575)
        val extremeHighlight = c(0xc3c3c3)
        override fun LayerList.createCoverSideLayers() {
            layer("topPart", highlight)
            layer("veesTop", shadow)
        }

        override fun LayerList.createTopLayers() {
            background(highlight)
            layer("borderShortDashes", color)
            layer("vees", shadow)
        }
    },
    PODZOL(c(0x6a4418),c(0x4a3018),c(0x8b5920)) {
        override fun LayerList.createCoverSideLayers() {
            layer("topPart", color)
            layer("zigzagBrokenTopPart", highlight)
        }

        override fun LayerList.createTopLayers() {
            background(color)
            layer("zigzagBroken", highlight)
            layer("borderDotted", shadow)
        }
    },
    MYCELIUM(c(0x6a656a),c(0x5a5952),c(0x7b6d73)) {
        override fun LayerList.createTopLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", shadow)
            layer("diagonalChecksBottomLeftTopRight", highlight)
            layer("diagonalOutlineChecksTopLeftBottomRight", highlight)
            layer("diagonalOutlineChecksBottomLeftTopRight", shadow)
        }
    },
    SNOW(Color.WHITE,  c(0xcfcfdf), Color.WHITE, nameOverrideTop = "snow", nameOverrideSide = "grass_block_snow") {
        override fun LayerList.createCoverSideLayers() {
            layer("topPart", color)
            layer("snowTopPart", shadow)
        }

        override fun LayerList.createTopLayers() {
            background(color)
            layer("snow", shadow)
        }
    }
    ;
    override val base = SimpleSoftEarth.DIRT
}