package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import io.github.pr0methean.ochd.texturebase.group
import javafx.scene.paint.Color

val COPPER_OXIDES = group<CopperOxide>()
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
    fun LayerListBuilder.commonLayers() {
        background(color)
        layer("streaks", highlight)
        layer("borderSolidTopLeft", highlight)
        layer("borderSolidBottomRight", shadow)
    }
    fun LayerListBuilder.uncut() {
        copy {commonLayers()}
        layer("copper2oxide", shadow)
    }
    fun LayerListBuilder.cut() {
        copy {commonLayers()}
        layer("cutInQuarters1", shadow)
        layer("cutInQuarters2", highlight)
    }
    override fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask> = listOf(
        ctx.out("block/${name}_copper") {uncut()},
        ctx.out("block/cut_${name}_copper") {cut()}
    )
    companion object {
        fun allOutputTasks(ctx: ImageProcessingContext) = values().flatMap { it.outputTasks(ctx) }
    }
}