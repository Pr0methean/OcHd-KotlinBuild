package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.FileOutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

val berryHighlight: Color = c(0xffff6b)
val berryColor: Color = c(0xff8931)
object CaveVines: ShadowHighlightMaterial {
    override val color: Paint = c(0x507233)
    override val shadow: Paint = c(0x4f3200)
    override val highlight: Paint = c(0x70922d)
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<FileOutputTask> = flow {
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
        emit(ctx.out(vinePlantTask, "block/cave_vines_plant"))
        emit(ctx.out(ctx.stack {
            copy(vinePlantTask)
            copy(berryTask)
        }, "block/cave_vines_plant_lit"))
        emit(ctx.out(vineTask, "block/cave_vines"))
        emit(ctx.out(ctx.stack {
            copy(vineTask)
            copy(berryTask)
        }, "block/cave_vines_lit"))
    }
}
