package io.github.pr0methean.ochd.color

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.OutputTask

interface SingleTextureMaterial: ShadowHighlightMaterial {
    val getTextureLayers: LayerList.() -> Unit
    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(outputTask(ctx))

    fun outputTask(ctx: ImageProcessingContext) = ctx.out(name, ctx.stack {getTextureLayers()})
}