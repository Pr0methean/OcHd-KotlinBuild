package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.*

val commandBlockDotColor: Color = c(0xc2873e)
private lateinit var sideBases: EnumMap<CommandBlock.SideType, ImageTask>

enum class CommandBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial {
    COMMAND_BLOCK(c(0xc77e4f),c(0xa66030),c(0xd7b49d)),
    CHAIN_COMMAND_BLOCK(c(0x76b297),c(0x5f8f7a),c(0xA8BEC5)) {
        override suspend fun LayerListBuilder.decorateBaseTexture() {
            layer("commandBlockChains")
        }
    },
    REPEATING_COMMAND_BLOCK(c(0x6a4fc7),c(0x553b9b),c(0x915431)) {
        override suspend fun LayerListBuilder.decorateBaseTexture() {
            layer("loopArrow")
        }
    };
    internal enum class SideType {
        FRONT {
            override suspend fun LayerListBuilder.createBase() {
                layer("commandBlockOctagon", Color.BLACK)
                layer("craftingGridSpacesCross", Color.WHITE)
            }

            override suspend fun LayerListBuilder.createFrame(i: Int) {
                layer("dotsInCross$i", commandBlockDotColor)
            }
        }, BACK {
            override suspend fun LayerListBuilder.createBase() {
                layer("commandBlockSquare", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, SIDE {
            override suspend fun LayerListBuilder.createBase() {
                layer("commandBlockArrowUnconditional", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        }, CONDITIONAL {
            override suspend fun LayerListBuilder.createBase() {
                layer("commandBlockArrow", Color.BLACK)
                layer("craftingGridSpaces", Color.WHITE)
            }
        };
        abstract suspend fun LayerListBuilder.createBase()
        open suspend fun LayerListBuilder.createFrame(i: Int) {
            layer("glider$i", commandBlockDotColor)
        }
    }

    private suspend fun LayerListBuilder.createBaseTexture() {
        background(color)
        layer("diagonalChecksTopLeftBottomRight", highlight)
        layer("diagonalChecksBottomLeftTopRight", highlight)
        layer("diagonalOutlineChecksTopLeftBottomRight", shadow)
        layer("diagonalOutlineChecksBottomLeftTopRight", shadow)
        decorateBaseTexture()
    }

    open suspend fun LayerListBuilder.decorateBaseTexture() {}

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        if (!::sideBases.isInitialized) {
            sideBases = EnumMap<SideType, ImageTask>(SideType::class.java)
            for (sideType in enumValues<SideType>()) {
                val sideBase = ctx.stack {sideType.run {createBase()}}
                val sideBasePerFrame = ctx.animate(listOf(sideBase, sideBase, sideBase, sideBase))
                val frames = Array(4) { index -> ctx.stack { sideType.run {createFrame(index)} }}.asList()
                val framesTask = ctx.animate(frames)
                sideBases[sideType] = ctx.stack {
                    copy(sideBasePerFrame)
                    copy(framesTask)
                }
            }
        }
        val baseTexture = ctx.stack {createBaseTexture()}
        val basePerFrame = ctx.animate(listOf(baseTexture, baseTexture, baseTexture, baseTexture))
        for (sideType in SideType.values()) {
            emit(ctx.out(ctx.stack {
                copy(basePerFrame)
                copy(sideBases[sideType]!!)
            }, "block/${name}_${sideType}"))
        }
    }
}