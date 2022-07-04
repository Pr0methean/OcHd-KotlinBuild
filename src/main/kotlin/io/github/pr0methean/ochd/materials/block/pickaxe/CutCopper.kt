@file:Suppress("ClassName")

package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore.COPPER
import io.github.pr0methean.ochd.texturebase.Block

object CUT_COPPER: Block {
    val nameOverride: String?
        get() = null
    override val name: String
        get() = nameOverride ?: this::class.simpleName!!

    override fun LayerListBuilder.createTextureLayers() {
        background(COPPER.color)
        layer("streaks", COPPER.highlight)
        layer("2x2BottomRight", COPPER.shadow, 0.5)
        layer("2x2TopLeft", COPPER.highlight, 0.5)
        layer("borderSolidTopLeft", COPPER.highlight)
        layer("borderSolidBottomRight", COPPER.shadow)
    }
}