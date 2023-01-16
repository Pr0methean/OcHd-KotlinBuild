package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask

interface DoubleTallBlock: Material {
    fun LayerListBuilder.createBottomLayers()

    fun LayerListBuilder.createTopLayers()

    val name: String

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out(ctx.stack { createBottomLayers() }, "block/${name}_bottom"))
        yield(ctx.out(ctx.stack { createTopLayers() }, "block/${name}_top"))
    }
}
