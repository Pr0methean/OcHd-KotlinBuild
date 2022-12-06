package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object ConcretePowder: DyedBlock("concrete_powder") {
    override suspend fun LayerListBuilder.createTextureLayers(color: Color, sharedLayers: ImageTask) {
        background(color)
        copy(sharedLayers)
    }

    override suspend fun createSharedLayersTask(ctx: TaskPlanningContext): ImageTask = ctx.stack {
        layer("checksSmall", DYES["gray"], 0.5)
        layer("checksSmall", DYES["light_gray"], 0.5)
    }
}
