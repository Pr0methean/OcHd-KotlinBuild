package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

enum class OreBase(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color,
    val orePrefix: String
) : Block, ShadowHighlightMaterial {

    STONE(c(0x888888), c(0x6d6d6d), c(0xa6a6a6), "") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(STONE.shadow)
            layer("checksLarge", STONE.highlight)
            layer("borderDotted", STONE.color)
        }
    },
    DEEPSLATE(c(0x515151), c(0x2f2f37), c(0x797979), "deepslate_") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalChecksBottomLeftTopRight", DEEPSLATE.highlight)
            layer("diagonalChecksTopLeftBottomRight", DEEPSLATE.shadow)
        }

        override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
            val baseTexture = ctx.stack {createTextureLayers()}
            emit(ctx.out("block/deepslate", baseTexture))
            emit(ctx.out("block/deepslate_bricks", ctx.stack {
                copy(baseTexture)
                layer("bricksSmall", shadow)
                layer("borderDotted", highlight)
                layer("borderDottedBottomRight", shadow)
            }))
            emit(ctx.out("block/deepslate_top", ctx.stack {
                copy(baseTexture)
                layer("cross", shadow)
                layer("borderSolid", highlight)
            }))
        }
    },
    NETHERRACK(c(0x723232), c(0x411616), c(0x854242), "nether_") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalOutlineChecksTopLeftBottomRight", NETHERRACK.shadow)
            layer("diagonalOutlineChecksBottomLeftTopRight", NETHERRACK.highlight)
        }
    };

    companion object {
        val stoneExtremeHighlight = c(0xb5b5b5)
        val stoneExtremeShadow = c(0x525252)
    }
}