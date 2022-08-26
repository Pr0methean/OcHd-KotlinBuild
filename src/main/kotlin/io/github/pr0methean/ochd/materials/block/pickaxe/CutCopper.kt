@file:Suppress("ClassName")

package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.COPPER
import io.github.pr0methean.ochd.texturebase.Block

object CutCopper: Block {
    override val name: String = "cut_copper"

    override suspend fun LayerListBuilder.createTextureLayers() {
        background(COPPER.color)
        layer("streaks", COPPER.highlight)
        layer("2x2BottomRight", COPPER.shadow)
        layer("2x2TopLeft", COPPER.highlight)
    }
}