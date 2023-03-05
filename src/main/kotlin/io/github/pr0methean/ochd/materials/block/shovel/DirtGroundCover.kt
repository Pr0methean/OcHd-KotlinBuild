package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth.POWDER_SNOW
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

private val grassItemColor = c(0x83b253)
private val grassItemShadow = c(0x64a43a)
@Suppress("unused")
private val grassItemHighlight = c(0x9ccb6c)

@Suppress("unused")
enum class DirtGroundCover(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial, GroundCoverBlock {
    /**
     * Grass is a gray texture, modified by a colormap according to the biome.
     */
    GRASS_BLOCK(c(0x9d9d9d), c(0x828282), c(0xbababa)) {
        val extremeShadow: Color = c(0x757575)
        val extremeHighlight: Color = c(0xc3c3c3)
        override fun LayerListBuilder.createTopLayers() {
            background(highlight)
            layer("borderShortDashes", color)
            layer("vees", shadow)
        }

        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", grassItemColor)
            layer("veesTop", grassItemShadow)
        }

        override fun OutputTaskBuilder.extraOutputTasks() {
            out("block/grass_block_side_overlay") {
                layer("topPart", color)
                layer("veesTop", shadow)
            }
        }
    },
    PODZOL(c(0x6a4418),c(0x4a3018),c(0x8b5920)) {
        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("zigzagBrokenTopPart", highlight)
        }

        override fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("zigzagBroken", highlight)
            layer("borderDotted", shadow)
        }

        override fun OutputTaskBuilder.outputTasks() {
            val top = stack { createTopLayers() }
            out("block/podzol_top", "block/composter_compost", source = top)
            out("block/composter_ready") {
                copy(top)
                layer("bonemealSmallNoBorder")
            }
            out("block/podzol_side") {
                copy(base)
                createCoverSideLayers()
            }
        }
    },
    MYCELIUM(c(0x6a656a),c(0x5a5a52),c(0x7b6d73)) {
        override fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", shadow)
            layer("diagonalChecksBottomLeftTopRight", highlight)
            layer("diagonalChecksFillTopLeftBottomRight", highlight)
            layer("diagonalChecksFillBottomLeftTopRight", shadow)
        }
        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("diagonalChecksTopLeft", shadow)
            layer("diagonalChecksTopRight", highlight)
            layer("diagonalChecksFillTopLeft", highlight)
            layer("diagonalChecksFillTopRight", shadow)
        }
    },
    SNOW(POWDER_SNOW.color, POWDER_SNOW.shadow, POWDER_SNOW.highlight) {
        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("snowTopPart", shadow)
        }

        override fun OutputTaskBuilder.outputTasks() {
            out("block/snow") { createTopLayers() }
            out("block/grass_block_snow") {
                copy(base)
                createCoverSideLayers()
            }
        }

        override fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("snow", shadow)
        }
    }
    ;
    override val base: SimpleSoftEarth = SimpleSoftEarth.DIRT
}
