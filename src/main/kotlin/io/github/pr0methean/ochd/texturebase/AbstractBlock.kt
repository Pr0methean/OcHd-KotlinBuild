package io.github.pr0methean.ochd.texturebase

abstract class AbstractBlock(val nameOverride: String? = null) : Block {
    override val name: String
        get() = nameOverride ?: this::class.simpleName!!
}