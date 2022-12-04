package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import javafx.application.Platform.requestNextPulse
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.joinAll
import java.util.*

class AnimationTask(
    val frames: List<Task<Image>>,
    val width: Int, val height: Int, name: String,
    cache: TaskCache<Image>,
    stats: ImageProcessingStats
): AbstractImageTask(name, cache, stats) {
    private val totalHeight = height * frames.size
    private val hashCode: Int by lazy {Objects.hash(frames, width, height)}

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
                other is AnimationTask
                        && other.width == width
                        && other.height == height
                        && other.frames == frames)
    }

    override fun hashCode(): Int = hashCode

    override suspend fun mergeWithDuplicate(other: Task<*>): ImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped !== other && deduped is AnimationTask && other is AnimationTask) {
            for ((index, frame) in deduped.frames.withIndex()) {
                frame.mergeWithDuplicate(deduped.frames[index])
            }
        }
        return deduped
    }

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("AnimationTask", name)
        val firstFrame = frames[0].await().getOrThrow()
        val canvas = Canvas(width.toDouble(), totalHeight.toDouble())
        val canvasCtx = canvas.graphicsContext2D
        canvasCtx.drawImage(firstFrame, 0.0, 0.0)
        val frameTasks = frames.withIndex().drop(1).map { (index, frameTask) ->
            frameTask.consumeAsync {
                canvasCtx.drawImage(it.getOrThrow(), 0.0, (height * index).toDouble())
            }
        }
        frameTasks.joinAll()
        val output = WritableImage(width, totalHeight)
        doJfx("Snapshot of $name") {
            requestNextPulse()
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        }
        if (output.isError) {
            throw output.exception
        }
        stats.onTaskCompleted("AnimationTask", name)
        return output
    }

    override val directDependencies: List<Task<Image>> = frames
}