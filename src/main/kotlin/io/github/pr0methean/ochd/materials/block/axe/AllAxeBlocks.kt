package io.github.pr0methean.ochd.materials.block.axe

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val AXE_BLOCKS = MaterialGroup(flow {
    emit(WOODS)
    emit(GIANT_MUSHROOMS)
    emit(SIMPLE_AXE_BLOCKS)
})