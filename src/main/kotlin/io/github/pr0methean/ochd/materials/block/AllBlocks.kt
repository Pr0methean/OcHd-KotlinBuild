package io.github.pr0methean.ochd.materials.block

import io.github.pr0methean.ochd.materials.block.axe.AXE_BLOCKS
import io.github.pr0methean.ochd.materials.block.barehand.BARE_HAND_BLOCKS
import io.github.pr0methean.ochd.materials.block.hoe.HOE_BLOCKS
import io.github.pr0methean.ochd.materials.block.indestructible.INDESTRUCTIBLE_BLOCKS
import io.github.pr0methean.ochd.materials.block.liquid.LIQUID_BLOCKS
import io.github.pr0methean.ochd.materials.block.pickaxe.PICKAXE_BLOCKS
import io.github.pr0methean.ochd.materials.block.shears.SHEAR_BLOCKS
import io.github.pr0methean.ochd.materials.block.shovel.SHOVEL_BLOCKS
import io.github.pr0methean.ochd.texturebase.MaterialGroup

val BLOCKS: MaterialGroup = MaterialGroup(
        AXE_BLOCKS, PICKAXE_BLOCKS, SHOVEL_BLOCKS, HOE_BLOCKS, SHEAR_BLOCKS, LIQUID_BLOCKS, BARE_HAND_BLOCKS,
        INDESTRUCTIBLE_BLOCKS)
