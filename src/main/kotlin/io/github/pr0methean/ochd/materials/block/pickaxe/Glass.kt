package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object Glass: Material {

    override fun outputTasks(ctx: ImageProcessingContext): Sequence<OutputTask> = sequence {
        DYES.forEach {
            val name = it.key
            val color = it.value
            yield(ctx.out("block/${name}_stained_glass", ctx.stack {
                background(Color(color.red, color.green, color.blue, 0.25))
                layer("borderSolid", color)
                layer("streaks", color)
            }))
            yield(ctx.out("block/${name}_stained_glass_pane_top", ctx.layer("paneTop", color)))
        }
        yield(ctx.out("block/glass_pane_top", ctx.layer("paneTop", c(0xa8d0d9))))
        yield(ctx.out("block/glass", ctx.stack {
            layer("borderSolid", Color.WHITE)
            layer("borderSolidBottomRight", DYES["gray"])
            layer("streaks", Color.WHITE)
        }))
        yield(ctx.out("block/tinted_glass", ctx.stack {
            background(Color(0.0,0.0,0.0,0.25))
            layer("borderSolid", Color.WHITE, 0.25)
            layer("streaks", Color.WHITE, 0.25)
        }))
    }
}