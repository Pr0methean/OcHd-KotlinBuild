package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.texturebase.MaterialGroup

val PICKAXE_BLOCKS = MaterialGroup(1,
                MaterialGroup(ORE_BASES, ORES, NYLIUMS),
                MaterialGroup(COPPER_OXIDES, CutCopper),
                MaterialGroup(StainedGlassFront, StainedGlassTop, Glass),
                SIMPLE_PICKAXE_BLOCKS, MiscRedstone, POLISHABLES, DyedTerracotta, Concrete, BoneBlock, Furnace, Rails)