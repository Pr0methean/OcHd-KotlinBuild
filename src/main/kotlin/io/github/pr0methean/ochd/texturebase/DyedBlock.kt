package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.scene.paint.Color

abstract class DyedBlock(val name: String): Material {
    abstract fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    )

    abstract fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        val sharedLayersTask = createSharedLayersTask(ctx)
        DYES.forEach { (dyeName, color) ->
            yield(ctx.out(ctx.stack {createTextureLayers(color, sharedLayersTask)}, "block/${dyeName}_$name"))
        }
    }
}
