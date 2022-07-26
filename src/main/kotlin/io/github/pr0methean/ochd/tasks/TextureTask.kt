package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.PackedImage

interface TextureTask: Task {

    suspend fun getImage(): PackedImage

    fun getImageNow(): PackedImage?
    override suspend fun run() {
        getImage()
    }
}