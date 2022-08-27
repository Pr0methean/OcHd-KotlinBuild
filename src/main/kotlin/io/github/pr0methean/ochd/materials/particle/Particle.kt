package io.github.pr0methean.ochd.materials.particle

import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.SingleLayerParticle

val NOTE = SingleLayerParticle("note", null, DirtGroundCover.GRASS_BLOCK.highlight)
val PARTICLES = MaterialGroup(NOTE)