package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.packedimage.ImageNode

interface TextureTask: Task {

    suspend fun getImage(): ImageNode

    fun getImageNow(): ImageNode?
    override suspend fun run() {
        getImage()
    }
}