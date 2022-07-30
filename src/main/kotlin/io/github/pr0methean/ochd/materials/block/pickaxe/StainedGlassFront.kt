package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.tasks.consumable.ConsumableImageTask
import io.github.pr0methean.ochd.tasks.consumable.OutputConsumableTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow

object StainedGlassFront: DyedBlock("stained_glass") {
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        layer(masterTask, color)
    }
    @Volatile lateinit var masterTask: ConsumableImageTask

    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputConsumableTask> {
        masterTask = ctx.stack {
            background(Color.BLACK, 0.25)
            layer("borderSolid", Color.BLACK)
            layer("streaks", Color.BLACK)
        }
        return super.outputTasks(ctx)
    }
}
