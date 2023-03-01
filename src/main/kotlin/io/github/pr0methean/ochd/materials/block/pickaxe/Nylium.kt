package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Paint

@Suppress("unused")
enum class Nylium(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial, GroundCoverBlock {
    CRIMSON_NYLIUM(c(0x854242), c(0x7b0000), c(0xbd3030)) {
        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("strokeTopLeftBottomRight2TopPart", shadow)
            layer("strokeBottomLeftTopRight2TopPart", highlight)
        }
        override fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", shadow)
            layer("strokeBottomLeftTopRight2", highlight)
            layer("borderLongDashes", highlight)
        }
    },
    WARPED_NYLIUM(c(0x568353), c(0x456b52), c(0xac2020)) {
        // SVGs allow the strokes to poke slightly outside the topPart rectangle
        override fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("strokeTopLeftBottomRight2TopPart", highlight)
            layer("strokeBottomLeftTopRight2TopPart", shadow)
        }

        override fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("strokeTopLeftBottomRight2", highlight)
            layer("strokeBottomLeftTopRight2", shadow)
            layer("borderShortDashes", shadow)
        }
    };
    override val base: OreBase = OreBase.NETHERRACK
    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask> = sequence {
        yield(ctx.out("block/${name}") { createTopLayers() }) // no "_top" at end
        yield(ctx.out("block/${name}_side") {
            copy(base)
            createCoverSideLayers()
        })
    }
}
