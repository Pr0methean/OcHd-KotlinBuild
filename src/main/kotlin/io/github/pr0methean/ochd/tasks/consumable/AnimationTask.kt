package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.joinAll
import java.util.*

class AnimationTask(
    val frames: List<ImageTask>,
    val width: Int, val height: Int, override val name: String,
    cache: TaskCache<Image>,
    override val stats: ImageProcessingStats
): AbstractImageTask(name, cache, stats) {
    private val totalHeight = height * frames.size

    override suspend fun clearFailure() {
        frames.asFlow().collect(Task<Image>::clearFailure)
        super.clearFailure()
    }

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
                other is AnimationTask
                        && other.frames == frames
                        && other.width == width
                        && other.height == height)
    }

    override fun hashCode(): Int {
        return Objects.hash(frames, width, height)
    }

    override suspend fun mergeWithDuplicate(other: Task<Image>): ImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped is AnimationTask && frames.size == deduped.frames.size) {
            for ((index, frame) in frames.withIndex()) {
                if (frame == deduped.frames[index]) {
                    frame.mergeWithDuplicate(deduped.frames[index])
                }
            }
        }
        return deduped
    }

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("AnimationTask", name)
        val canvas = Canvas(width.toDouble(), totalHeight.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        val frameTasks = frames.map { it }.mapIndexed { index, frameTask -> frameTask.consumeAsync {
            canvasCtx.drawImage(it.getOrThrow(), 0.0, (height * index).toDouble())
        }}
        frames.asFlow().collect { it.startAsync() }
        frameTasks.joinAll()
        val output = WritableImage(width, totalHeight)
        doJfx("Snapshot of $name") {
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        }
        if (output.isError) {
            throw output.exception
        }
        stats.onTaskCompleted("AnimationTask", name)
        return output
    }

}