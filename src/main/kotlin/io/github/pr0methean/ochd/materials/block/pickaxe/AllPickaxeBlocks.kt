package io.github.pr0methean.ochd.materials.block.pickaxe

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.group

val PICKAXE_BLOCKS: MaterialGroup = MaterialGroup(
    group<OreBase>(), group<Ore>(), group<Nylium>(), group<CopperOxide>(), CutCopper, StainedGlassFront, Glass,
    MiscRedstone, group<Polishable>(), StainedGlassTop, group<SimplePickaxeBlock>(), DyedTerracotta, Concrete,
    BoneBlock, Furnace, Rails)