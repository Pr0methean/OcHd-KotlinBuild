package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

//  9 SvgImportTask
//  8 RepaintTask for background
// 12 RepaintTask for grid background
//  3 ImageStackingTask for background
// 12 ImageStackingTask for finished command block
// --
// 44 total tasks
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
            layer("loopArrow4x")
        }
    };
    internal enum class SideType {
        FRONT {
            override suspend fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockOctagon4x", shadow)
                layer("commandBlockGridFront")
            }
        }, BACK {
            override suspend fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockSquare4x", shadow)
                layer("commandBlockGrid")
            }
        }, SIDE {
            override suspend fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockArrowUnconditional4x", shadow)
                layer("commandBlockGrid")
            }
        }, CONDITIONAL {
            override suspend fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockArrow4x", shadow)
                layer("commandBlockGrid")
            }
        };
        abstract suspend fun LayerListBuilder.createGrid(shadow: Paint)
    }

    private suspend fun LayerListBuilder.createBackground() {
        background(color)
        layer("diagonalChecks4x", highlight)
        layer("diagonalOutlineChecks4x", shadow)
        decorateBackground()
    }

    open suspend fun LayerListBuilder.decorateBackground() {}

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        val background = ctx.stack {createBackground()}
        for (sideType in enumValues<SideType>()) {
            emit(ctx.out(ctx.stack {sideType.run {
                copy(background)
                createGrid(shadow)
            }}, "block/${name}_${sideType}"))
        }
    }
}