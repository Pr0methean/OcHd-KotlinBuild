package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val INDESTRUCTIBLE_BLOCKS = MaterialGroup(flow {
    emit(COMMAND_BLOCKS)
    emit(STRUCTURE_AND_JIGSAW_BLOCKS)
    emit(SIMPLE_INDESTRUCTIBLE_BLOCKS)
})