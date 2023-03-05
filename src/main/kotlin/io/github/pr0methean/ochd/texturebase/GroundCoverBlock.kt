package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String

    fun LayerListBuilder.createCoverSideLayers()
    fun LayerListBuilder.createTopLayers()

    fun OutputTaskBuilder.extraOutputTasks() {}

    override fun OutputTaskBuilder.outputTasks() {
        out("block/${this@GroundCoverBlock.name}_top") { createTopLayers() }
        out("block/${this@GroundCoverBlock.name}_side") {
            copy(base)
            createCoverSideLayers()
        }
        extraOutputTasks()
    }
}
