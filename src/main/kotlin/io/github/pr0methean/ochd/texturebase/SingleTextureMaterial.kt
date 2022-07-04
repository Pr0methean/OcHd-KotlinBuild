package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.OutputTask

fun LayerList.copy(source: SingleTextureMaterial) {
    source.copyTo(this)
}
interface SingleTextureMaterial: Material {
    val directory: String

    val name: String

    fun LayerList.createTextureLayers()

    fun copyTo(dest: LayerList) {
        dest.copy(LayerList(dest.ctx).apply {createTextureLayers()})
    }
    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(
        ctx.out("$directory/$name", ctx.stack { createTextureLayers() })
    )
}