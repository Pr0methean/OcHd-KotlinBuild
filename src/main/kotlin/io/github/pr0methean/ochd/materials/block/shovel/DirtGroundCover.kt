package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

/*
grass_item_h='9ccb6c'
grass_item='83b253'
grass_item_s='64a43a'
 */
private val grassItemColor = c(0x83b253)
private val grassItemShadow = c(0x64a43a)
@Suppress("unused")
private val grassItemHighlight = c(0x9ccb6c)

@Suppress("unused")
enum class DirtGroundCover(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint,
    override val nameOverrideTop: String? = null,
    override val nameOverrideSide: String? = null
): ShadowHighlightMaterial, GroundCoverBlock {
    /**
     * Grass is a gray texture, modified by a colormap according to the biome.
     */
    GRASS_BLOCK(c(0x9d9d9d), c(0x828282), c(0xbababa)) {
        val extremeShadow = c(0x757575)
        val extremeHighlight = c(0xc3c3c3)
        override suspend fun LayerListBuilder.createTopLayers() {
            background(highlight)
            layer("borderShortDashes", color)
            layer("vees", shadow)
        }

        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", grassItemColor)
            layer("veesTop", grassItemShadow)
        }

        override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask>
                = merge(super.outputTasks(ctx), flowOf(ctx.out("block/grass_block_side_overlay", ctx.stack {
            layer("topPart", color)
            layer("veesTop", shadow)
        })))
    },
    PODZOL(c(0x6a4418),c(0x4a3018),c(0x8b5920)) {
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("zigzagBrokenTopPart", highlight)
        }

        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("zigzagBroken", highlight)
            layer("borderDotted", shadow)
        }
    },
    MYCELIUM(c(0x6a656a),c(0x5a5952),c(0x7b6d73)) {
        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", shadow)
            layer("diagonalChecksBottomLeftTopRight", highlight)
            layer("diagonalOutlineChecksTopLeftBottomRight", highlight)
            layer("diagonalOutlineChecksBottomLeftTopRight", shadow)
        }
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("diagonalChecksTopLeft", shadow)
            layer("diagonalChecksTopRight", highlight)
            layer("diagonalOutlineChecksTopLeft", highlight)
            layer("diagonalOutlineChecksTopRight", shadow)
        }
    },
    SNOW(Color.WHITE,  c(0xcfcfdf), Color.WHITE, nameOverrideTop = "snow", nameOverrideSide = "grass_block_snow") {
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("snowTopPart", shadow)
        }

        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("snow", shadow)
        }
    }
    ;
    override val base = SimpleSoftEarth.DIRT
}