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
import kotlinx.coroutines.flow.flow

val BLOCKS = MaterialGroup(flow {
        emit(AXE_BLOCKS)
        emit(PICKAXE_BLOCKS)
        emit(SHOVEL_BLOCKS)
        emit(HOE_BLOCKS)
        emit(SHEAR_BLOCKS)
        emit(LIQUID_BLOCKS)
        emit(BARE_HAND_BLOCKS)
        emit(INDESTRUCTIBLE_BLOCKS)
})