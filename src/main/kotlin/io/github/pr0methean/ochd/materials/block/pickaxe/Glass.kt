package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Color

object Glass: Material {
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out("block/glass_pane_top", ctx.layer("paneTop", c(0xa8d5d5))))
        yield(ctx.out("block/glass") {
            layer("borderSolid", c(0x515151))
            layer("borderSolidTopLeft", Color.WHITE)
            layer("streaks", Color.WHITE)
        })
        yield(ctx.out("block/tinted_glass", ctx.layer({
            background(Color.BLACK)
            layer("borderSolid", Color.WHITE)
            layer("streaks", Color.WHITE)
        }, alpha = 0.25)))
    }
}
