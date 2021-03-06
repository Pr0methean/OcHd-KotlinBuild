package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

val commandBlockDotColor = c(0xc2873e)

enum class CommandBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    COMMAND_BLOCK(c(0xc77e4f),c(0xa66030),c(0xd7b49d)),
    CHAIN_COMMAND_BLOCK(c(0x76b297),c(0x5f8f7a),c(0xa1c3b4)) {
        override fun LayerListBuilder.decorateBaseTexture() {
            layer("commandBlockChains")
        }
    },
    REPEATING_COMMAND_BLOCK(c(0x6a4fc7),c(0x553b9b),c(0x9b8bcf)) {
        override fun LayerListBuilder.decorateBaseTexture() {
            layer("loopArrow")
        }
    };
    private enum class SideType {
        FRONT {
            override fun LayerListBuilder.createBase() {
                layer("commandBlockOctagon", Color.BLACK)
                layer("craftingGridSpacesCross", Color.WHITE)
            }

            override fun LayerListBuilder.createFrame(i: Int) {
                layer("dotsInCross$i", commandBlockDotColor)
            }
        }, BACK {
            override fun LayerListBuilder.createBase() {
                layer("commandBlockSquare", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, SIDE {
            override fun LayerListBuilder.createBase() {
                layer("commandBlockArrowUnconditional", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, CONDITIONAL {
            override fun LayerListBuilder.createBase() {
                layer("commandBlockArrow", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        };
        abstract fun LayerListBuilder.createBase()
        open fun LayerListBuilder.createFrame(i: Int) {
            layer("glider$i", commandBlockDotColor)
        }
    }

    fun LayerListBuilder.createBaseTexture() {
        background(color)
        layer("diagonalChecksTopLeftBottomRight", highlight)
        layer("diagonalChecksBottomLeftTopRight", highlight)
        layer("diagonalOutlineChecksTopLeftBottomRight", shadow)
        layer("diagonalOutlineChecksBottomLeftTopRight", shadow)
        decorateBaseTexture()
    }

    open fun LayerListBuilder.decorateBaseTexture() {}

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val baseTexture = ctx.stack {createBaseTexture()}
        for (sideType in SideType.values()) {
            val sideBase = ctx.stack {sideType.run {createBase()}}
            val frames = Array(4) { index -> ctx.stack {
                copy(baseTexture)
                copy(sideBase)
                sideType.run {createFrame(index)}
            }}.asList()
            emit(ctx.out("block/${name}_${sideType}", ctx.animate(frames)))
        }
    }
}