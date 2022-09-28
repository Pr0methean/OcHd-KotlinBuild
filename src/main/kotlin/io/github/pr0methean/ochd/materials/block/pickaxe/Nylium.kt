package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
enum class Nylium(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial, GroundCoverBlock {
    CRIMSON_NYLIUM(c(0x854242), c(0x7b0000), c(0xbd3030)) {
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("strokeTopLeftBottomRight2TopPart", shadow)
            layer("strokeBottomLeftTopRight2TopPart", highlight)
        }
        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", shadow)
            layer("strokeBottomLeftTopRight2", highlight)
            layer("borderLongDashes", highlight)
        }
    },
    WARPED_NYLIUM(c(0x568353), c(0x456b52), c(0xac2020)) {
        // SVGs allow the strokes to poke slightly outside the topPart rectangle
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("strokeTopLeftBottomRight2TopPart", highlight)
            layer("strokeBottomLeftTopRight2TopPart", shadow)
        }

        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("borderShortDashes", shadow)
        }
    };
    override val base: OreBase = OreBase.NETHERRACK
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        emit(ctx.out(ctx.stack { createTopLayers() }, "block/${name}")) // no "_top" at end
        emit(ctx.out(ctx.stack {
            copy(base)
            createCoverSideLayers()
        }, "block/${name}_side"))
    }
}