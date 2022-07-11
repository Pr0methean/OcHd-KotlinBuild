package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val BARE_HAND_BLOCKS = MaterialGroup(flow {
        emit(Tnt)
        emit(Wool)
        emit(Torch)
        emit(CROPS)
        emit(BIOME_COLORIZED_PLANTS)
        emit(SIMPLE_BARE_HAND_BLOCKS)
        emit(DOUBLE_TALL_FLOWERS)
})