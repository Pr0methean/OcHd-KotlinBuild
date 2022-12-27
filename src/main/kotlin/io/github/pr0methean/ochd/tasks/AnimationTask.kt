package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.DEFAULT_SNAPSHOT_PARAMS
import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.application.Platform.requestNextPulse
import javafx.scene.image.Image
import javafx.scene.image.WritableImage
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("AnimationTask")

/**
 * Task that stacks the input images in a column. Minecraft can use this as an animated texture with the input images as
 * frames.
 */
class AnimationTask(
    val background: AbstractImageTask,
    val frames: List<AbstractImageTask>, val width: Int, val height: Int,
    name: String,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext,
    stats: ImageProcessingStats
): AbstractImageTask(name, cache, ctx, stats) {
    private val totalHeight = height * frames.size
    private val hashCode: Int by lazy { Objects.hash(frames, width, height) }
    private val dependencies = frames + background

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
                other is AnimationTask
                        && other.width == width
                        && other.height == height
                        && other.frames == frames)
    }

    override fun hashCode(): Int = hashCode

    override suspend fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        val deduped = super.mergeWithDuplicate(other)
        if (deduped !== other && deduped is AnimationTask && other is AnimationTask) {
            for ((index, frame) in deduped.frames.withIndex()) {
                frame.mergeWithDuplicate(deduped.frames[index])
            }
        }
        return deduped
    }

    @Suppress("DeferredResultUnused")
    override suspend fun render(): Image {
        stats.onTaskLaunched("AnimationTask", name)
        val backgroundImage = background.await()
        background.removeDirectDependentTask(this)
        logger.info("Allocating a canvas for {}", name)
        val canvasMutex = Mutex()
        val canvas = getCanvas(width, totalHeight)
        val canvasCtx = canvas.graphicsContext2D
        for (index in frames.indices) {
            canvasCtx.drawImage(backgroundImage, 0.0, (height * index).toDouble())
        }
        val frameTasks = frames.withIndex().map { (index, frameTask) ->
            coroutineScope.launch {
                canvasMutex.withLock {
                    frameTask.renderOnto(canvasCtx, 0.0, (height * index).toDouble())
                }
                frameTask.removeDirectDependentTask(this@AnimationTask)
            }
        }
        frameTasks.joinAll()
        val output = WritableImage(width, totalHeight)
        doJfx("Snapshot of $name") {
            requestNextPulse()
            canvas.snapshot(DEFAULT_SNAPSHOT_PARAMS, output)
        }
        logger.info("Canvas is now unreachable for {}", name)
        if (output.isError) {
            throw output.exception
        }
        stats.onTaskCompleted("AnimationTask", name)
        return output
    }

    override val directDependencies: List<AbstractTask<Image>> = dependencies
}
