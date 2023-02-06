package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.Material
import io.github.pr0methean.ochd.times
import javafx.scene.paint.Color

object Glass: Material {
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out(ctx.layer("paneTop", c(0xa8d5d5)), "block/glass_pane_top"))
        yield(ctx.out(ctx.stack {
            layer("borderSolid", c(0x515151))
            layer("borderSolidTopLeft", Color.WHITE)
            layer("streaks", Color.WHITE)
        }, "block/glass"))
        yield(ctx.out(ctx.stack {
            background(Color.BLACK * 0.25)
            layer("borderSolid", Color.WHITE, 0.25)
            layer("streaks", Color.WHITE, 0.25)
        }, "block/tinted_glass"))
    }
}
