package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.packedimage.PackedImage
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.DyedBlock
import javafx.scene.paint.Color
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow

object StainedGlassFront: DyedBlock("stained_glass") {
    @Suppress("DeferredResultUnused")
    override fun LayerListBuilder.createTextureLayers(color: Color) {
        layer(masterTask, color)
    }
    lateinit var masterTask: Deferred<PackedImage>

    @Suppress("DeferredResultUnused")
    override fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> {
        masterTask = ctx.stack {
            background(Color.BLACK, 0.25)
            layer("borderSolid", Color.BLACK)
            layer("streaks", Color.BLACK)
        }
        return super.outputTasks(ctx)
    }
}
