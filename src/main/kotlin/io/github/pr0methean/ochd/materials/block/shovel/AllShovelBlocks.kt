package io.github.pr0methean.ochd.materials.block.shovel

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val SHOVEL_BLOCKS = MaterialGroup(flow {
    emit(SIMPLE_SOFT_EARTH_BLOCKS)
    emit(DIRT_GROUND_COVERS)
    emit(ConcretePowder)
})