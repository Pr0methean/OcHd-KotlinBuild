package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.isShallowCopyOf
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import javafx.scene.image.Image
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
@Suppress("EqualsWithHashCodeExist", "EqualsOrHashCode")
class AnimationTask(
    val background: AbstractImageTask,
    val frames: List<AbstractImageTask>,
    width: Int,
    private val frameHeight: Int,
    name: String,
    cache: DeferredTaskCache<Image>,
    ctx: CoroutineContext
): AbstractImageTask(name, cache, ctx, width, frameHeight * frames.size) {
    private val dependencies = frames + background

    override fun computeHashCode(): Int = Objects.hash(background, frames, width, height)

    override fun equals(other: Any?): Boolean {
        return (this === other) || (
                other is AnimationTask
                        && other.width == width
                        && other.height == height
                        && other.background == background
                        && other.frames == frames)
    }

    override fun mergeWithDuplicate(other: AbstractTask<*>): AbstractImageTask {
        if (this === other) {
            return this
        }
        if (other is AnimationTask && (background !== other.background || frames !== other.frames)) {
            logger.debug("Merging AnimationTask {} with duplicate {}", name, other.name)
            val mergedFrames = frames.zip(other.frames).map { (a, b) -> if (a === b) a else a.mergeWithDuplicate(b) }
            val mergedBackground = background.mergeWithDuplicate(other.background)
            if (mergedBackground !== background || !mergedFrames.isShallowCopyOf(frames)) {
                return AnimationTask(
                    mergedBackground, mergedFrames,
                    width, height, name, cache, ctx
                )
            }
        }
        return super.mergeWithDuplicate(other)
    }

    @Suppress("DeferredResultUnused")
    override suspend fun perform(): Image {
        ImageProcessingStats.onTaskLaunched("AnimationTask", name)
        val backgroundImage = background.await()
        background.removeDirectDependentTask(this)
        logger.info("Allocating a canvas for {}", name)
        val canvasMutex = Mutex()
        val canvas = createCanvas()
        val canvasCtx = canvas.graphicsContext2D
        for (index in frames.indices) {
            canvasCtx.drawImage(backgroundImage, 0.0, (frameHeight * index).toDouble())
        }
        val frameTasks = frames.withIndex().map { (index, frameTask) ->
            coroutineScope.launch {
                canvasMutex.withLock {
                    frameTask.renderOnto({ canvasCtx }, 0.0, (frameHeight * index).toDouble())
                }
                frameTask.removeDirectDependentTask(this@AnimationTask)
            }
        }
        frameTasks.joinAll()
        val output = snapshotCanvas(canvas)
        ImageProcessingStats.onTaskCompleted("AnimationTask", name)
        return output
    }

    override val directDependencies: List<AbstractTask<Image>> = dependencies
}
