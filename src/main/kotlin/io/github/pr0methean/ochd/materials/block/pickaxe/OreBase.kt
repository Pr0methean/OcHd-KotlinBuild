package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
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
    STONE(c(0x888888), c(0x737373), c(0xaaaaaa), "") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(STONE.shadow)
            layer("checksLarge", STONE.highlight)
            layer("borderDotted", STONE.color)
        }
    },
    DEEPSLATE(c(0x515151), c(0x2f2f3f), c(0x737373), "deepslate_") {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("diagonalChecksBottomLeftTopRight", DEEPSLATE.highlight)
            layer("diagonalChecksTopLeftBottomRight", DEEPSLATE.shadow)
        }

        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
            val baseTexture = ctx.stack {createTextureLayers()}
            emit(ctx.out(baseTexture, "block/deepslate"))
            emit(ctx.out(ctx.stack {
                copy(baseTexture)
                layer("bricksSmall", shadow)
                layer("borderDotted", highlight)
                layer("borderDottedBottomRight", shadow)
            }, "block/deepslate_bricks"))
            emit(ctx.out(ctx.stack {
                copy(baseTexture)
                layer("cross", shadow)
                layer("borderSolid", highlight)
            }, "block/deepslate_top"))
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
        val stoneExtremeHighlight = c(0xaaaaaa)
        val stoneExtremeShadow = c(0x515151)
    }
}