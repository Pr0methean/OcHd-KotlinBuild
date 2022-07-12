package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.PackedImage

interface TextureTask {
    fun isComplete(): Boolean

    fun willExpandHeap(): Boolean

    suspend fun getImage(): PackedImage
}