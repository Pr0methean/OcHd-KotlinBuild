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

class AnimationConsumableTask(
    val frames: List<ConsumableImageTask>,
    val width: Int, val height: Int, override val name: String,
    val cache: TaskCache<Image>,
    override val stats: ImageProcessingStats
): AbstractConsumableImageTask(name, cache, stats) {
    private val totalHeight = height * frames.size

    override suspend fun clearFailure() {
        frames.map(ConsumableImageTask::unpacked).asFlow().collect(ConsumableTask<Image>::clearFailure)
        super.clearFailure()
    }

    @Suppress("DeferredResultUnused")
    override suspend fun startPrerequisites() {
        frames.asFlow().collect(ConsumableImageTask::startAsync)
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

    override suspend fun mergeWithDuplicate(other: ConsumableTask<Image>): ConsumableImageTask {
        if (other is AnimationConsumableTask) {
            for ((index, frame) in frames.withIndex()) {
                if (frame == other.frames[index]) {
                    frame.mergeWithDuplicate(other.frames[index])
                }
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        stats.onTaskLaunched("AnimationConsumableTask", name)
        val canvas = Canvas(width.toDouble(), totalHeight.toDouble())
        canvas.isCache = true
        val canvasCtx = canvas.graphicsContext2D
        val frameTasks = frames.map(ConsumableImageTask::unpacked).mapIndexed {index, frameTask -> frameTask.consumeAsync {
            doJfx("Frame $index of $name") {
                canvasCtx.drawImage(it.getOrThrow(), 0.0, (height * index).toDouble())
            }
        }}
        frames.asFlow().collect { it.startAsync() }
        frameTasks.joinAll()
        val output = WritableImage(width, totalHeight)
        return doJfx("Snapshot of $name") {
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
            canvas.isCache = false
            if (output.isError) {
                throw output.exception
            }
            output
        }
    }

}