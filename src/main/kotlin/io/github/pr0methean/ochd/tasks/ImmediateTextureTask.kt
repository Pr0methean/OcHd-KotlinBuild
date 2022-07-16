package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.PackedImage

data class ImmediateTextureTask(val image: PackedImage): TextureTask {
    override fun isComplete(): Boolean = true

    override fun willExpandHeap(): Boolean = false

    override suspend fun getImage(): PackedImage = image
}