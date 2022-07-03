package io.github.pr0methean.ochd.materials.pickaxe

import io.github.pr0methean.ochd.materials.pickaxe.Ore.COPPER
import io.github.pr0methean.ochd.texturebase.AbstractSingleTextureMaterial

object CutCopper: AbstractSingleTextureMaterial(
    color = COPPER.color,
    shadow = COPPER.shadow,
    highlight = COPPER.highlight,
    name = "block/cut_copper",
    getTextureLayers = {
        background(COPPER.color)
        layer("streaks", COPPER.highlight)
        layer("borderSolid", COPPER.shadow)
        layer("borderSolidTopLeft", COPPER.highlight)
        layer("cutInQuarters1", COPPER.shadow)
        layer("cutInQuarters2", COPPER.highlight)
    }
)