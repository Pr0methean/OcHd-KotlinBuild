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

val commandBlockDotColor: Color = c(0xc2873e)

@Suppress("unused")
enum class CommandBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    COMMAND_BLOCK(c(0xc77e4f),c(0xa66030),c(0xd7b49d)),
    CHAIN_COMMAND_BLOCK(c(0x76b297),c(0x5f8f7a),c(0xA8BEC5)) {
        override suspend fun LayerListBuilder.decorateBackground() {
            layer("commandBlockChains4x")
        }
    },
    REPEATING_COMMAND_BLOCK(c(0x6a4fc7),c(0x553b9b),c(0x915431)) {
        override suspend fun LayerListBuilder.decorateBackground() {
            layer("loopArrow4x", Color.WHITE)
        }
    };
    internal enum class SideType {
        FRONT {
            override suspend fun LayerListBuilder.createBase(shadow: Paint) {
                layer("commandBlockOctagon", shadow)
                layer("craftingGridSpacesCross", Color.WHITE)
            }

            override suspend fun LayerListBuilder.createFrames() {
                layer("dotsInCrossAll", commandBlockDotColor)
            }
        }, BACK {
            override suspend fun LayerListBuilder.createBase(shadow: Paint) {
                layer("commandBlockSquare", shadow)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, SIDE {
            override suspend fun LayerListBuilder.createBase(shadow: Paint) {
                layer("commandBlockArrowUnconditional", shadow)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, CONDITIONAL {
            override suspend fun LayerListBuilder.createBase(shadow: Paint) {
                layer("commandBlockArrow", shadow)
                layer("craftingGridSpaces", Color.WHITE)
            }
        };
        abstract suspend fun LayerListBuilder.createBase(shadow: Paint)
        open suspend fun LayerListBuilder.createFrames() {
            layer("gliderAll", commandBlockDotColor)
        }
    }

    private suspend fun LayerListBuilder.createBackground() {
        background(color)
        layer("diagonalChecks4x", highlight)
        layer("diagonalOutlineChecks4x", shadow)
        decorateBackground()
    }

    open suspend fun LayerListBuilder.decorateBackground() {}

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        for (sideType in enumValues<SideType>()) {
            emit(ctx.out(ctx.stack {sideType.run {
                createBackground()
                val basePerFrame = ctx.stack {createBase(shadow)}
                copy(ctx.animate(List(4) {basePerFrame}))
                createFrames()
            }}, "block/${name}_${sideType}"))
        }
    }
}