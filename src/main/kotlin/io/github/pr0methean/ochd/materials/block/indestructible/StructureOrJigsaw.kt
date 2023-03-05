package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color

@Suppress("unused")
enum class StructureOrJigsaw(private val foregroundLayer: String?) {
    JIGSAW_BOTTOM(null),
    JIGSAW_TOP("jigsaw"),
    JIGSAW_SIDE("arrowUp"),
    JIGSAW_LOCK("jigsawLock"),
    STRUCTURE_BLOCK("bigCircle"),
    STRUCTURE_BLOCK_CORNER("cornerCrosshairs"),
    STRUCTURE_BLOCK_DATA("data"),
    STRUCTURE_BLOCK_LOAD("folderLoad") {
        override fun LayerListBuilder.createTextureLayers(backgroundAndBorder: AbstractImageTask) {
            copy {
                copy(backgroundAndBorder)
                layer("folder", color)
            }
            layer("loadArrow", highlight)
        }
    },
    STRUCTURE_BLOCK_SAVE("folderSave") {
        override fun LayerListBuilder.createTextureLayers(backgroundAndBorder: AbstractImageTask) {
            copy {
                copy(backgroundAndBorder)
                layer("folder", color)
            }
            layer("saveArrow", highlight)
        }
    };

    val color: Color = c(0xb493b4)
    val shadow: Color = c(0x26002a)
    val highlight: Color = c(0xd7c2d7)
    open fun LayerListBuilder.createTextureLayers(backgroundAndBorder: AbstractImageTask) {
        copy(backgroundAndBorder)
        foregroundLayer?.let { layer(it, highlight) }
    }

    protected fun LayerListBuilder.backgroundAndBorder() {
        copy {
            background(shadow)
            layer("borderDotted", color)
        }
    }
}

object StructureAndJigsaw: ShadowHighlightMaterial {
    override val color: Color = c(0xb493b4)
    override val shadow: Color = c(0x26002a)
    override val highlight: Color = c(0xd7c2d7)
    override fun OutputTaskBuilder.outputTasks() {
        val backgroundAndBorder = stack {
            background(shadow)
            layer("borderDotted", color)
        }
        StructureOrJigsaw.values().forEach {
            out("block/${it.name}") { it.run { this@out.createTextureLayers(backgroundAndBorder) }}
        }
    }
}
