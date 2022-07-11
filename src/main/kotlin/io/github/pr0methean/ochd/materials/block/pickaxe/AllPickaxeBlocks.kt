package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import kotlinx.coroutines.flow.flow

val PICKAXE_BLOCKS = MaterialGroup(flow {
        emit(ORE_BASES)
        emit(ORES)
        emit(COPPER_OXIDES)
        emit(CutCopper)
        emit(StainedGlassFront)
        emit(StainedGlassTop)
        emit(NYLIUMS)
        emit(POLISHABLES)
        emit(Concrete)
        emit(BoneBlock)
        emit(Rails)
        emit(MiscRedstone)
        emit(Furnace)
        emit(Glass)
        emit(SIMPLE_PICKAXE_BLOCKS)
})