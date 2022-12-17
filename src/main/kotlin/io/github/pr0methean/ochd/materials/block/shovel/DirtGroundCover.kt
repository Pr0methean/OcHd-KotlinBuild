package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.shovel.SimpleSoftEarth.POWDER_SNOW
import io.github.pr0methean.ochd.tasks.PngOutputTask
import io.github.pr0methean.ochd.texturebase.GroundCoverBlock
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.merge

private val grassItemColor = c(0x83b253)
private val grassItemShadow = c(0x64a43a)
@Suppress("unused")
private val grassItemHighlight = c(0x9ccb6c)

@Suppress("unused")
enum class DirtGroundCover(
    override val color: Paint,
    override val shadow: Paint,
    override val highlight: Paint
): ShadowHighlightMaterial, GroundCoverBlock {
    /**
     * Grass is a gray texture, modified by a colormap according to the biome.
     */
    GRASS_BLOCK(c(0x9d9d9d), c(0x828282), c(0xbababa)) {
        val extremeShadow: Color = c(0x757575)
        val extremeHighlight: Color = c(0xc3c3c3)
        override suspend fun LayerListBuilder.createTopLayers() {
            background(highlight)
            layer("borderShortDashes", color)
            layer("vees", shadow)
        }

        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", grassItemColor)
            layer("veesTop", grassItemShadow)
        }

        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<PngOutputTask>
                = merge(super.outputTasks(ctx), flowOf(ctx.out(ctx.stack {
            layer("topPart", color)
            layer("veesTop", shadow)
        }, "block/grass_block_side_overlay")))
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

        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<PngOutputTask> = flow {
            val top = ctx.stack { createTopLayers() }
            emit(ctx.out(top, arrayOf("block/podzol_top", "block/composter_compost")))
            emit(ctx.out(ctx.stack {
                copy(top)
                layer("bonemealSmallNoBorder")
            }, "block/composter_ready"))
            emit(ctx.out(ctx.stack {
                copy(base)
                createCoverSideLayers()
            }, "block/podzol_side"))
        }
    },
    MYCELIUM(c(0x6a656a),c(0x5a5a52),c(0x7b6d73)) {
        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("diagonalChecksTopLeftBottomRight", shadow)
            layer("diagonalChecksBottomLeftTopRight", highlight)
            layer("diagonalChecksFillTopLeftBottomRight", highlight)
            layer("diagonalChecksFillBottomLeftTopRight", shadow)
        }
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("diagonalChecksTopLeft", shadow)
            layer("diagonalChecksTopRight", highlight)
            layer("diagonalChecksFillTopLeft", highlight)
            layer("diagonalChecksFillTopRight", shadow)
        }
    },
    SNOW(POWDER_SNOW.color,  POWDER_SNOW.shadow, POWDER_SNOW.highlight) {
        override suspend fun LayerListBuilder.createCoverSideLayers() {
            layer("topPart", color)
            layer("snowTopPart", shadow)
        }

        override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<PngOutputTask> = flow {
            emit(ctx.out(ctx.stack { createTopLayers() }, "block/snow"))
            emit(ctx.out(ctx.stack {
                copy(base)
                createCoverSideLayers()
            }, "block/grass_block_snow"))
        }

        override suspend fun LayerListBuilder.createTopLayers() {
            background(color)
            layer("snow", shadow)
        }
    }
    ;
    override val base: SimpleSoftEarth = SimpleSoftEarth.DIRT
}
