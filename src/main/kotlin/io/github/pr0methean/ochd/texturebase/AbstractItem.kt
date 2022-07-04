package io.github.pr0methean.ochd.texturebase

abstract class AbstractItem(val nameOverride: String? = null) : Item {
    override val name: String
        get() = nameOverride ?: this::class.simpleName!!
}