@file:Suppress("ClassName")

package io.github.pr0methean.ochd.materials.item

import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.MaterialGroup
import io.github.pr0methean.ochd.texturebase.SingleLayerItem

val REMAINS: MaterialGroup = MaterialGroup(BONE, BONE_MEAL)

object BONE : SingleLayerItem("boneBottomLeftTopRight", "bone", c(0xeaead0))

object BONE_MEAL: SingleLayerItem("bonemealSmall", "bone_meal")

// TODO: Rotten flesh
