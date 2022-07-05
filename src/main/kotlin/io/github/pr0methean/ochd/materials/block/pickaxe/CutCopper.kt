@file:Suppress("ClassName")

package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.COPPER
import io.github.pr0methean.ochd.texturebase.Block

object CutCopper: Block {
    override val name: String = "cut_copper"

    override fun LayerListBuilder.createTextureLayers() {
        background(COPPER.color)
        layer("streaks", COPPER.highlight)
        layer("2x2BottomRight", COPPER.shadow, 0.5)
        layer("2x2TopLeft", COPPER.highlight, 0.5)
        layer("borderSolid", COPPER.shadow)
        layer("borderSolidTopLeft", COPPER.highlight)
    }
}