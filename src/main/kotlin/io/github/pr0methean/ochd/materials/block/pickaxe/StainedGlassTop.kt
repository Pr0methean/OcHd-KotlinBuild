package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object StainedGlassTop : DyedBlock("stained_glass_pane_top") {
    override fun LayerListBuilder.createTextureLayers(
        color: Color,
        sharedLayers: AbstractImageTask
    ) {
        layer(sharedLayers, color)
    }

    override fun createSharedLayersTask(ctx: TaskPlanningContext): AbstractImageTask = ctx.layer("paneTop")
}
