package io.github.pr0methean.ochd.materials.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerList
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.OutputTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color

enum class CopperOxide(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color) : ShadowHighlightMaterial {
    /*
    exposed_copper_h='ce8d83'
exposed_copper=''
exposed_copper_s=''
weathered_copper_h='7ab799'
weathered_copper=''
weathered_copper_s=''
oxidized_copper_h='6ec59f'
oxidized_copper=''
oxidized_copper_s='3b5c5c'
     */
    EXPOSED(
        color = c(0xa87762),
        shadow = c(0x796454),
        highlight = c(0xa87762)
    ),
    WEATHERED(
        color = c(0x64a077),
        shadow = c(0x6a7147),
        highlight = c(0x7ab799)
    ),
    OXIDIZED(
        color = c(0x4fab90),
        shadow = c(0x3b5c5c),
        highlight = c(0x6ec59f)
    );
    fun LayerList.commonLayers() {
        background(color)
        layer("streaks", highlight)
        layer("borderSolidTopLeft", highlight)
        layer("borderSolidBottomRight", shadow)
    }
    fun LayerList.uncut() {
        copy {commonLayers()}
        layer("copper2oxide", shadow)
    }
    fun LayerList.cut() {
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