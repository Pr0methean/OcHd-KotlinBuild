package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object ConcretePowder: DyedBlock("concrete_powder") {
    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }

    override fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask =
        ctx.stack {
            layer("checksSmall", c(0x515151), 0.5)
            layer("checksSmall", c(0xaaaaaa), 0.5)
        }
}
