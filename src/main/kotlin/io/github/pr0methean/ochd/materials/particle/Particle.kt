package io.github.pr0methean.ochd.materials.particle

import io.github.pr0methean.ochd.materials.block.shovel.DirtGroundCover
import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.SingleLayerParticle

val Note = SingleLayerParticle("note", "note", DirtGroundCover.GRASS.highlight)
val PARTICLES = MaterialGroup(Note)