package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

/**
 * This is the only material that feeds an ImageStackingTask to a RepaintTask rather than vice-versa.
 */
object StainedGlassFront: DyedBlock("stained_glass") {
    override suspend fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        layer(sharedLayers, color)
    }

    override suspend fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.stack {
        background(Color.BLACK, 0.25)
        layer("borderSolid", Color.BLACK)
        layer("streaks", Color.BLACK)
    }
}
