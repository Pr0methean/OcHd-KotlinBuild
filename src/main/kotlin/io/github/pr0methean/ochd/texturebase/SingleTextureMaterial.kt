package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.OutputTask

interface SingleTextureMaterial: ShadowHighlightMaterial {
    val nameOverride: String?
    val getTextureLayers: LayerList.() -> Unit
    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(outputTask(ctx))

    fun outputTask(ctx: ImageProcessingContext) = ctx.out(nameOverride ?: name, ctx.stack {getTextureLayers()})
}