package io.github.pr0methean.ochd.texturebase

interface Item : SingleTextureMaterial {
    override val directory: String
        get() = "item"
}