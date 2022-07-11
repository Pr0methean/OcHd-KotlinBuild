package io.github.pr0methean.ochd.materials

import io.github.pr0methean.ochd.materials.block.BLOCKS
import io.github.pr0methean.ochd.materials.item.ITEMS
import io.github.pr0methean.ochd.materials.particle.PARTICLES
import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val ALL_MATERIALS = MaterialGroup(flow{
    emit(BLOCKS)
    emit(ITEMS)
    emit(PARTICLES)
})