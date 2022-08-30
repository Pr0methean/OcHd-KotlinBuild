package io.github.pr0methean.ochd.materials.block.indestructible

import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.group

val INDESTRUCTIBLE_BLOCKS =
    MaterialGroup(group<StructureOrJigsaw>(), group<SimpleIndestructibleBlock>(), group<CommandBlock>())