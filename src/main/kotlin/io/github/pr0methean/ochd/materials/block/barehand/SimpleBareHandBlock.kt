package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.axe.GiantMushroom
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color.WHITE
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

@Suppress("unused")
enum class SimpleBareHandBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    SUGARCANE(c(0xaadb74), c(0x82a859), c(0x91ff32)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("bambooThick", shadow)
            layer("bambooThin", highlight)
            layer("bambooThinMinusBorder", color)
        }
    },
    BROWN_MUSHROOM(GiantMushroom.BROWN_MUSHROOM_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("mushroomStem", GiantMushroom.MUSHROOM_STEM.color)
            layer("mushroomCapBrown", color)
        }
    },
    RED_MUSHROOM(GiantMushroom.RED_MUSHROOM_BLOCK) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("mushroomStem", GiantMushroom.MUSHROOM_STEM.color)
            layer("mushroomCapRed", color)
        }
    },
    // Dust is stored as white, and changed to black or red in real time, with the shade depending on power level
    REDSTONE_DUST_DOT(WHITE, WHITE, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("redstoneDot", color)
        }
    },
    REDSTONE_DUST_LINE(WHITE, WHITE, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            layer("redstoneLine", color)
        }

        override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> {
            val layers = ctx.stack {createTextureLayers()}
            return flowOf(ctx.out(layers, "block/redstone_dust_line0", "block/redstone_dust_line1"))
        }
    };
    
    constructor(base: ShadowHighlightMaterial) : this(base.color, base.shadow, base.highlight)
}