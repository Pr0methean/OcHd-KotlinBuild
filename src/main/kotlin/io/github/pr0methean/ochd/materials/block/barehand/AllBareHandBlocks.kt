package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.group

val BARE_HAND_BLOCKS: MaterialGroup = MaterialGroup(
    CaveVines, Tnt, Wool, Torch, group<Crop>(), group<BiomeColorizedPlant>(), group<SimpleBareHandBlock>(),
    group<DoubleTallFlower>()
)