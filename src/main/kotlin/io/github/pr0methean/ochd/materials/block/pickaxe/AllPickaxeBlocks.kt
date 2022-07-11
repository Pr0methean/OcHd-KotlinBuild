package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val PICKAXE_BLOCKS = MaterialGroup(flow {
        emit(ORE_BASES)
        emit(ORES)
        emit(COPPER_OXIDES)
        emit(NYLIUMS)
        emit(POLISHABLES)
        emit(SIMPLE_PICKAXE_BLOCKS)
        emit(CutCopper)
        emit(StainedGlassFront)
        emit(StainedGlassTop)
        emit(Concrete)
        emit(BoneBlock)
        emit(Rails)
        emit(MiscRedstone)
        emit(Furnace)
        emit(Glass)
})