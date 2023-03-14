package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.tasks.AbstractImageTask
import io.github.pr0methean.ochd.texturebase.ShadowHighlightMaterial
import javafx.scene.paint.Color

@Suppress("unused")
enum class CopperOxide(
    override val color: Color,
    override val shadow: Color,
    override val highlight: Color) : ShadowHighlightMaterial {
    EXPOSED(
        color = c(0xa87762),
        shadow = c(0x795B4B),
        highlight = c(0xce8888)
    ),
    WEATHERED(
        color = c(0x64a077),
        shadow = c(0x647147),
        highlight = c(0x74BE9C)
    ),
    OXIDIZED(
        color = c(0x4fab90),
        shadow = c(0x3b5c5c),
        highlight = c(0x74BE9C)
    );
    private fun LayerListBuilder.commonLayers() {
        background(color)
        layer("borderSolid", shadow)
        layer("streaks", highlight)
        layer("borderSolidTopLeft", highlight)
    }
    private fun LayerListBuilder.uncut(commonLayers: AbstractImageTask) {
        copy(commonLayers)
        layer("copper2oxide", shadow)
    }
    private fun LayerListBuilder.cut(commonLayers: AbstractImageTask) {
        copy(commonLayers)
        layer("cutInQuarters1", shadow)
        layer("cutInQuarters2", highlight)
    }
    override fun OutputTaskEmitter.outputTasks() {
        val commonLayers = stack { commonLayers() }
        out("block/${this@CopperOxide.name}_copper") { uncut(commonLayers) }
        out("block/cut_${this@CopperOxide.name}_copper") { cut(commonLayers) }
    }
}
