package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String
    val nameOverrideTop: String?
            get() = null
    val nameOverrideSide: String?
            get() = null

    fun LayerListBuilder.createCoverSideLayers() {
        copyTopOf {createTopLayers()}
    }
    fun LayerListBuilder.createTopLayers()

    override fun rawOutputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out("block/${nameOverrideTop ?: "${name}_top"}", ctx.stack { createTopLayers() }))
        emit(ctx.out("block/${nameOverrideSide ?: "${name}_side"}", ctx.stack {
            copy(base)
            createCoverSideLayers()
        }))
    }
}
