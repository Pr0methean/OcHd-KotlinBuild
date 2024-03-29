package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import javafx.scene.paint.Color

abstract class DyedBlock(val name: String): Material {
    abstract fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    )

    abstract fun createSharedLayersTask(ctx: OutputTaskEmitter): AbstractImageTask

    override fun OutputTaskEmitter.outputTasks() {
        val sharedLayersTask = createSharedLayersTask(this@outputTasks)
        DYES.forEach { (dyeName, color) ->
            out("block/${dyeName}_${this@DyedBlock.name}") { createTextureLayers(color, sharedLayersTask) }
        }
    }
}
