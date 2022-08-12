package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.consumable.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Suppress("unused")
enum class CopperOxide(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color) : ShadowHighlightMaterial {
    EXPOSED(
        color = c(0xa87762),
        shadow = c(0x796454),
        highlight = c(0xce8d83)
    ),
    WEATHERED(
        color = c(0x64a077),
        shadow = c(0x647147),
        highlight = c(0x7ab799)
    ),
    OXIDIZED(
        color = c(0x4fab90),
        shadow = c(0x3b5c5c),
        highlight = c(0x6ec59f)
    );
    private suspend fun LayerListBuilder.commonLayers() {
        background(color)
        layer("streaks", highlight)
        layer("borderSolid", shadow)
        layer("borderSolidTopLeft", highlight)
    }
    private suspend fun LayerListBuilder.uncut() {
        copy {commonLayers()}
        layer("copper2oxide", shadow)
    }
    private suspend fun LayerListBuilder.cut() {
        copy {commonLayers()}
        layer("cutInQuarters1", shadow)
        layer("cutInQuarters2", highlight)
    }
    override suspend fun outputTasks(ctx: ImageProcessingContext): Flow<OutputTask> = flow {
        emit(ctx.out({ uncut() }, "block/${name}_copper"))
        emit(ctx.out({ cut() }, "block/cut_${name}_copper"))
    }
}