package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object Glass: Material {
    override fun OutputTaskBuilder.outputTasks() {
        out("block/glass_pane_top", layer("paneTop", c(0xa8d5d5)))
        out("block/glass") {
            layer("borderSolid", c(0x515151))
            layer("borderSolidTopLeft", Color.WHITE)
            layer("streaks", Color.WHITE)
        }
        out("block/tinted_glass", layer({
            background(Color.BLACK)
            layer("borderSolid", Color.WHITE)
            layer("streaks", Color.WHITE)
        }, alpha = 0.25))
    }
}
