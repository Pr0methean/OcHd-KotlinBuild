package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter

interface DoubleTallBlock: Material {
    fun LayerListBuilder.createBottomLayers()

    fun LayerListBuilder.createTopLayers()

    val name: String

    fun OutputTaskEmitter.extraOutputTasks() {}

    override fun OutputTaskEmitter.outputTasks() {
        out("block/${this@DoubleTallBlock.name}_bottom") { createBottomLayers() }
        out("block/${this@DoubleTallBlock.name}_top") { createTopLayers() }
        extraOutputTasks()
    }
}
