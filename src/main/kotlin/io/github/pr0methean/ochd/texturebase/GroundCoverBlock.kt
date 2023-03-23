package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter

interface GroundCoverBlock: Material {
    val base: SingleTextureMaterial
    val name: String

    fun LayerListBuilder.createCoverSideLayers()
    fun LayerListBuilder.createTopLayers()

    fun OutputTaskEmitter.extraOutputTasks() {
        // No-op by default. Overridden to add more output tasks when necessary.
    }

    override fun OutputTaskEmitter.outputTasks() {
        out("block/${this@GroundCoverBlock.name}_top") { createTopLayers() }
        out("block/${this@GroundCoverBlock.name}_side") {
            copy(base)
            createCoverSideLayers()
        }
        extraOutputTasks()
    }
}
