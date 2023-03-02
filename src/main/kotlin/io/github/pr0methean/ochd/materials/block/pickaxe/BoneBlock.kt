package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object BoneBlock: Material {
    val color: Color = c(0xe1ddca)
    val shadow: Color = c(0xc3bfa1)
    val highlight: Color = c(0xEaEaD0)
    override suspend fun OutputTaskBuilder.outputTasks() {
        out("block/bone_block_top") {
            background(shadow)
            layer("borderSolid", highlight)
            layer("boneBottomLeftTopRightNoCross", highlight)
            layer("boneTopLeftBottomRightNoCross", color)
        }
        out("block/bone_block_side") {
            background(color)
            layer("borderSolid", shadow)
            layer("borderDotted", highlight)
            layer("boneBottomLeftTopRightNoCross", shadow)
            layer("boneTopLeftBottomRightNoCross", highlight)
        }
    }
}
