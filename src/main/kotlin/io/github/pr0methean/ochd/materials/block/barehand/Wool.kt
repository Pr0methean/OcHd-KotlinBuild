package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object Wool : DyedBlock("wool") {
    override fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.stack {
        layer("zigzagBroken", Color.BLACK, 0.25)
        layer("borderSolid", Color.BLACK, 0.25)
        layer("zigzagBroken2", Color.WHITE, 0.25)
        layer("borderDotted", Color.WHITE, 0.25)
    }

    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        background(color)
        copy(sharedLayers)
    }
}
