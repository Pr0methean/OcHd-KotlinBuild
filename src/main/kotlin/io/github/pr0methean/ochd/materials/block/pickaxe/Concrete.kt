package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Concrete: DyedBlock("concrete") {
    override suspend fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }

    override suspend fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.stack {
        layer("strokeBottomLeftTopRight", DYES["gray"], 0.25)
        layer("strokeTopLeftBottomRight", DYES["gray"], 0.25)
        layer("borderShortDashes", DYES["light_gray"], 0.25)
    }
}
