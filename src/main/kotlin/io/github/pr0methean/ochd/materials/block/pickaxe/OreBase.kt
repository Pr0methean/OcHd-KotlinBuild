package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color

enum class OreBase(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val orePrefix: String,
    override val hasOutput: Boolean = true
) : Block, ShadowHighlightMaterial {
    STONE(c(0x888888), c(0x737373), c(0xaaaaaa), "") {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("checksQuarterCircles", highlight)
            layer("checksQuarterCircles2", shadow)
            layer("sunflowerPistil", color)
        }
    },
    DEEPSLATE(c(0x515151), c(0x2f2f3f), c(0x737373), "deepslate_") {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalChecksBottomLeftTopRight", DEEPSLATE.highlight)
            layer("diagonalChecksTopLeftBottomRight", DEEPSLATE.shadow)
        }

        override fun OutputTaskEmitter.outputTasks() {
            val baseTexture = stack { createTextureLayers() }
            out("block/deepslate", baseTexture)
            out("block/deepslate_bricks") {
                copy(baseTexture)
                layer("bricksSmall", shadow)
                layer("borderDotted", highlight)
                layer("borderDottedBottomRight", shadow)
            }
            out("block/deepslate_top") {
                copy(baseTexture)
                layer("cross", shadow)
                layer("borderSolid", highlight)
            }
        }
    },
    NETHERRACK(c(0x723232), c(0x410000), c(0x854242), "nether_") {
        override fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", shadow)
            layer("diagonalChecksBottomLeftTopRight", highlight)
            layer("diagonalChecksFillTopLeftBottomRight", color)
            layer("diagonalChecksFillBottomLeftTopRight", color)
        }
    };

    companion object {
        val stoneExtremeHighlight: Color = c(0xaaaaaa)
        val stoneExtremeShadow: Color = c(0x515151)
    }
}
