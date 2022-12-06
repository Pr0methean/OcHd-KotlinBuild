package io.github.pr0methean.ochd.texturebase

interface Block : SingleTextureMaterial {
    override val directory: String
        get() = "block"
}
