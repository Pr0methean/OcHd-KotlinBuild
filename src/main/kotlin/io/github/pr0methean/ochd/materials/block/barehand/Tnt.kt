package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
    override val color = c(0xdb2f00)
    override val shadow = c(0x912d00)
    override val highlight = c(0xff4300)

    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        val tntBottom = ctx.stack {
            background(Color.BLACK)
            layer("tntSticksEnd", color)
        }
        emit(ctx.out(ctx.stack {
            background(shadow)
            layer("tntSticksSide", color)
            layer("borderDotted", highlight)
            layer("tntStripe", Color.WHITE)
            layer("tntSign", Color.BLACK)
        }, "block/tnt_side"))
        emit(ctx.out(tntBottom, "block/tnt_bottom"))
        emit(ctx.out(ctx.stack {
            copy(tntBottom)
            layer("tntFuzes", Color.BLACK)
        }, "block/tnt_top"))
    }
}