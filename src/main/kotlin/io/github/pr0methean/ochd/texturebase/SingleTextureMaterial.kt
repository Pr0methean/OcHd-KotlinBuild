package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter

interface SingleTextureMaterial: Material {
    val directory: String
    val hasOutput: Boolean
    val name: String

    fun LayerListBuilder.createTextureLayers()

    fun copyTo(dest: LayerListBuilder) {
        dest.apply {createTextureLayers()}
    }
    override fun OutputTaskEmitter.outputTasks() {
        if (hasOutput) {
            out("$directory/${this@SingleTextureMaterial.name}") { createTextureLayers() }
        }
    }
}
