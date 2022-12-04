package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color

object StainedGlassTop : DyedBlock("stained_glass_pane_top") {
    override suspend fun LayerListBuilder.createTextureLayers(color: Color, sharedLayers: ImageTask) {
        layer(sharedLayers, color)
    }

    override suspend fun createSharedLayersTask(ctx: TaskPlanningContext): ImageTask = ctx.layer("paneTop")
}