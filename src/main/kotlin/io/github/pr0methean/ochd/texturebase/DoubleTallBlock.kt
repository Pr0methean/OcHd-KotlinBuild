package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder

interface DoubleTallBlock: Material {
    fun LayerListBuilder.createBottomLayers()

    fun LayerListBuilder.createTopLayers()

    val name: String

    fun OutputTaskBuilder.extraOutputTasks() {}

    override fun OutputTaskBuilder.outputTasks() {
        out("block/${this@DoubleTallBlock.name}_bottom") { createBottomLayers() }
        out("block/${this@DoubleTallBlock.name}_top") { createTopLayers() }
        extraOutputTasks()
    }
}
