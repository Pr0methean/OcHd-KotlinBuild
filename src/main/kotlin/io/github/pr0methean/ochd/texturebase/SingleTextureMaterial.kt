package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask

interface SingleTextureMaterial: Material {
    val directory: String

    val name: String

    fun LayerListBuilder.createTextureLayers()

    fun copyTo(dest: LayerListBuilder) {
        dest.copy(LayerListBuilder(dest.ctx).apply {createTextureLayers()}.build())
    }
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequenceOf(
        ctx.out(ctx.stack { createTextureLayers() }, "$directory/$name")
    )
}
