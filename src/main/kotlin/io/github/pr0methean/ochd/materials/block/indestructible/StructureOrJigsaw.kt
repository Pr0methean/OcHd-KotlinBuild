package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import io.github.pr0methean.ochd.texturebase.group

/*
structure_block_bg='26002a'
structure_block_fg='d7c2d7'
 */
val jigsawBackground = c(0x26002a)
val jigsawForeground = c(0xd7c2d7)
val STRUCTURE_AND_JIGSAW_BLOCKS = group<StructureOrJigsaw>()
enum class StructureOrJigsaw(val foregroundLayer: String?): SingleTextureMaterial, Block {
    JIGSAW_BOTTOM(null),
    JIGSAW_TOP("jigsaw"),
    JIGSAW_SIDE("arrowUp"),
    JIGSAW_LOCK("jigsawLock"),
    STRUCTURE_BLOCK("bigCircle"),
    STRUCTURE_BLOCK_CORNER("cornerCrosshairs"),
    STRUCTURE_BLOCK_DATA("data"),
    STRUCTURE_BLOCK_LOAD("folderLoad"),
    STRUCTURE_BLOCK_SAVE("folderSave");

    override fun LayerListBuilder.createTextureLayers() {
        copy {
            background(jigsawBackground)
            layer("borderDotted", jigsawForeground, 0.25)
        }
        foregroundLayer?.let { layer(it, jigsawForeground) }
    }
}