package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.group

val PICKAXE_BLOCKS = MaterialGroup(
    group<OreBase>(), group<Ore>(), group<Nylium>(), group<CopperOxide>(), CutCopper, StainedGlassFront,
    StainedGlassTop, Glass, group<SimplePickaxeBlock>(), MiscRedstone, group<Polishable>(), DyedTerracotta, Concrete,
    BoneBlock, Furnace, Rails)