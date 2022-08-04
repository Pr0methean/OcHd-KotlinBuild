package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.pickaxe.SimplePickaxeBlock
import io.github.pr0methean.ochd.texturebase.Block
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.SingleTextureMaterial
import javafx.scene.paint.Color.BLACK
import javafx.scene.paint.Color.WHITE
import javafx.scene.paint.Paint

@Suppress("unused")
enum class SimpleIndestructibleBlock(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): SingleTextureMaterial, ShadowHighlightMaterial, Block {
    BEDROCK(c(0x575757), c(0x222222), c(0x979797)) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            background(color)
            layer("borderSolid", shadow)
            layer("strokeTopLeftBottomRight2", shadow)
            layer("strokeBottomLeftTopRight2", highlight)
        }
    },
    END_PORTAL_FRAME_SIDE(StructureOrJigsaw.JIGSAW_BOTTOM.shadow, BLACK, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(SimplePickaxeBlock.END_STONE)
            layer("endPortalFrameSide", color)
        }
    },
    END_PORTAL_FRAME_TOP(StructureOrJigsaw.JIGSAW_BOTTOM.shadow, BLACK, WHITE) {
        override suspend fun LayerListBuilder.createTextureLayers() {
            copy(SimplePickaxeBlock.END_STONE)
            layer("endPortalFrameTop", color)
            layer("railDetector", shadow)
        }
    };
}