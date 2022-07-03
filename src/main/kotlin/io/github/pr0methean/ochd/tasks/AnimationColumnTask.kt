package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.LayerList
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.CoroutineScope

data class AnimationColumnTask(private val frames: LayerList,
                               private val size: Int,
                               override val scope: CoroutineScope): ImageCombiningTask(frames, size, scope) {
    override fun doBlockingJfx(input: List<Image>): Image {
        val canvas = Canvas(size.toDouble(), (size * frames.size).toDouble())
        val canvasCtx = canvas.graphicsContext2D
        for ((index, frame) in input.withIndex()) {
            canvasCtx.drawImage(frame, 0.0, (size * index).toDouble())
        }
        val out = WritableImage(size, size * frames.size)
        canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, out)
        return out
    }
}
