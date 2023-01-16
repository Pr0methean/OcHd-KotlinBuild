package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint

val berryHighlight: Color = c(0xffff6b)
val berryColor: Color = c(0xff8931)
object CaveVines: ShadowHighlightMaterial {
    override val color: Paint = c(0x507233)
    override val shadow: Paint = c(0x4f3200)
    override val highlight: Paint = c(0x70922d)
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val vinePlantTask = ctx.stack {
            layer("wavyVines", shadow)
            layer("waves", highlight)
        }
        val vineTask = ctx.stack {
            layer("wavyVinesBottom", shadow)
            layer("wavesBottom", highlight)
        }
        val berryTask = ctx.stack {
            layer("vineBerries", berryColor)
            layer("vineBerriesHighlight", berryHighlight)
        }
        yield(ctx.out(vinePlantTask, "block/cave_vines_plant"))
        yield(ctx.out(ctx.stack {
            copy(vinePlantTask)
            copy(berryTask)
        }, "block/cave_vines_plant_lit"))
        yield(ctx.out(vineTask, "block/cave_vines"))
        yield(ctx.out(ctx.stack {
            copy(vineTask)
            copy(berryTask)
        }, "block/cave_vines_lit"))
    }
}
