package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.flow.asFlow
import java.util.*

data class AnimationConsumableTask(
    val frames: List<ConsumableImageTask>,
    val width: Int, val height: Int, override val name: String,
    val cache: TaskCache<Image>,
    override val stats: ImageProcessingStats
): AbstractConsumableImageTask(name, cache, stats) {
    val totalHeight = height * frames.size

    override suspend fun clearFailure() {
        frames.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::clearFailure)
        super.clearFailure()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
                other is AnimationConsumableTask
                        && other.frames == frames
                        && other.width == width
                        && other.height == height)
    }

    override fun hashCode(): Int {
        return Objects.hash(frames, width, height)
    }

    override suspend fun perform(): Image {
        val canvas = Canvas(width.toDouble(), totalHeight.toDouble())
        canvas.isCache = true
        val canvasCtx = canvas.graphicsContext2D
        frames.map(ConsumableImageTask::unpacked).forEachIndexed {index, frameTask -> frameTask.consume {
            doJfx("Frame $index of $name") {
                canvasCtx.drawImage(it.getOrThrow(), 0.0, (height * index).toDouble())
            }
        }}
        frames.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::await)
        val output = WritableImage(width, totalHeight)
        return doJfx("snapshot for $name") {
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
            if (output.isError) {
                throw output.exception
            }
            output
        }
    }

}