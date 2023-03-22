package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Paint

//  9 SvgToBitmapTask
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
        override fun LayerListBuilder.decorateBackground() {
            layer("commandBlockChains4x")
        }
    },
    REPEATING_COMMAND_BLOCK(c(0x6a4fc7),c(0x553b9b),c(0x9b8bcf)) {
        override fun LayerListBuilder.decorateBackground() {
            layer("loopArrow4x")
        }
    };
    internal enum class SideType {
        FRONT {
            override fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockOctagon4x", shadow)
                layer("commandBlockGridFront")
            }
        }, BACK {
            override fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockSquare4x", shadow)
                layer("commandBlockGrid")
            }
        }, SIDE {
            override fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockArrowUnconditional4x", shadow)
                layer("commandBlockGrid")
            }
        }, CONDITIONAL {
            override fun LayerListBuilder.createGrid(shadow: Paint) {
                layer("commandBlockArrow4x", shadow)
                layer("commandBlockGrid")
            }
        };
        abstract fun LayerListBuilder.createGrid(shadow: Paint)
    }

    private fun LayerListBuilder.createBackground() {
        background(color)
        layer("diagonalChecks4x", shadow)
        layer("diagonalChecksFill4x", highlight)
        decorateBackground()
    }

    open fun LayerListBuilder.decorateBackground() {
        // No-op by default; extra background layer is added in an override if needed.
    }

    override fun OutputTaskEmitter.outputTasks() {
        val background = stack { createBackground() }
        for (sideType in enumValues<SideType>()) {
            out("block/${this@CommandBlock.name}_${sideType}") { sideType.run {
                this@out.copy(background)
                this@out.createGrid(shadow)
            }}
        }
    }
}
