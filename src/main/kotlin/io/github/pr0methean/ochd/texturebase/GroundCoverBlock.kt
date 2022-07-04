package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.tasks.OutputTask

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String
    val nameOverrideTop: String?
            get() = null
    val nameOverrideSide: String?
            get() = null

    fun LayerList.createCoverSideLayers() {
        copyTopOf {createTopLayers()}
    }
    fun LayerList.createTopLayers()

    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(
        ctx.out("block/${nameOverrideTop ?: "${name}_top"}", ctx.stack { createTopLayers() }),
        ctx.out("block/${nameOverrideSide ?: "${name}_side"}", ctx.stack {
            copy(base)
            createCoverSideLayers()
        })
    )
}
