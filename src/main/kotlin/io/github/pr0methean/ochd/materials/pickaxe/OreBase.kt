package io.github.pr0methean.ochd.materials.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color

enum class OreBase(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val orePrefix: String,
    override val getTextureLayers: LayerList.() -> Unit
) : SingleTextureMaterial {
    STONE(c(0x888888), c(0x6d6d6d), c(0xa6a6a6), "", {
        background(STONE.shadow)
        layer("checksLarge", STONE.highlight)
        layer("borderDotted", STONE.color)
    }) {
        val extremeHighlight = c(0xb5b5b5)
        val extremeShadow = c(0x525252)
    },
    DEEPSLATE(c(0x515151), c(0x2f2f37), c(0x797979), "deepslate_", {
        layer("diagonalChecksBottomLeftTopRight", DEEPSLATE.highlight)
        layer("diagonalChecksTopLeftBottomRight", DEEPSLATE.shadow)
    }),
    NETHERRACK(c(0x723232), c(0x411616), c(0x854242), "nether_", {
        layer("diagonalOutlineChecksTopLeftBottomRight", NETHERRACK.shadow)
        layer("diagonalOutlineChecksBottomLeftTopRight", NETHERRACK.highlight)
    });

    companion object {
        fun allOutputTasks(ctx: ImageProcessingContext) = values().map {it.outputTask(ctx)}
    }
}