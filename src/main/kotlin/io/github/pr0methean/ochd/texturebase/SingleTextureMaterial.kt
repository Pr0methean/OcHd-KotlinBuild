package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder

interface SingleTextureMaterial: Material {
    val directory: String
    val hasOutput: Boolean
    val name: String

    fun LayerListBuilder.createTextureLayers()

    fun copyTo(dest: LayerListBuilder) {
        dest.apply {createTextureLayers()}
    }
    override suspend fun OutputTaskBuilder.outputTasks() {
        if (hasOutput) {
            out("$directory/${this@SingleTextureMaterial.name}") { createTextureLayers() }
        }
    }
}
