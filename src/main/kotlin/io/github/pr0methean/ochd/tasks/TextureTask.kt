package io.github.pr0methean.ochd.tasks

import javafx.scene.image.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async

abstract class TextureTask(private val scope: CoroutineScope) {
    private val coroutine = scope.async(start = CoroutineStart.LAZY) {
        println("Starting task ${this@TextureTask}")
        val bitmap = computeBitmap()
        println("Finished task ${this@TextureTask}")
        return@async bitmap
    }

    abstract suspend fun computeBitmap(): Image

    suspend fun getBitmap(): Image = coroutine.await()
}