package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String

    fun LayerListBuilder.createCoverSideLayers()
    fun LayerListBuilder.createTopLayers()

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out(ctx.stack { createTopLayers() }, "block/${name}_top"))
        yield(ctx.out(ctx.stack {
            copy(base)
            createCoverSideLayers()
        }, "block/${name}_side"))
    }
}
