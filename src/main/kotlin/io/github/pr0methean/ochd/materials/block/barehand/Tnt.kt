package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color

/*
push tntSticksSide ${tnt} tnt10 ${tnt_s}
push borderDotted ${tnt_h} tnt15
push tntStripe ${white} tnt20
push tntSign ${black} tnt30
out_stack block/tnt_side

out_layer tntSticksEnd ${red} block/tnt_bottom ${black}

push_copy block/tnt_bottom tnt1
push tntFuzes ${black} tnt2
out_stack block/tnt_top
 */
object Tnt: ShadowHighlightMaterial {
    override val color: Color = c(0xdb2f00)
    override val shadow: Color = c(0x912d00)
    override val highlight: Color = c(0xff4300)

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val tntBottom = ctx.stack {
            background(Color.BLACK)
            layer("tntSticksEnd", color)
        }
        yield(ctx.out("block/tnt_side") {
            background(shadow)
            layer("tntSticksSide", color)
            layer("borderDotted", highlight)
            layer("tntStripe", Color.WHITE)
            layer("tntSign")
        })
        yield(ctx.out("block/tnt_bottom", tntBottom))
        yield(ctx.out("block/tnt_top") {
            copy(tntBottom)
            layer("tntFuzes")
        })
    }
}
