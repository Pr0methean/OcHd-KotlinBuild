package io.github.pr0methean.ochd.tasks

import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope

data class AnimationColumnTask(private val frames: List<TextureTask>,
                               private val size: Int,
                               private val scope: CoroutineScope): TextureTask(scope) {
    override suspend fun computeBitmap(): Image {
        val canvas = Canvas(size.toDouble(), (size * frames.size).toDouble())
        val canvasCtx = canvas.graphicsContext2D
        for ((index, frame) in frames.withIndex()) {
            canvasCtx.drawImage(frame.getBitmap(), 0.0, (size * index).toDouble())
        }
        val out = WritableImage(size, size * frames.size)
        canvas.snapshot(null, out)
        return out
    }

}
