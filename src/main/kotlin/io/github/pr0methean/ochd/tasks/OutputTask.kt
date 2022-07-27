package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.StrongAsyncLazy
import io.github.pr0methean.ochd.packedimage.PackedImage
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.File
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

private val logger = LogManager.getLogger("OutputTask")
class OutputTask(private val producer: TextureTask,
                 override val name: String,
                 private val file: File,
                 val stats: ImageProcessingStats,
): Task<Unit> {
    @Volatile private var started: Boolean = false
    @Volatile private var completed: Boolean = false

    val result = StrongAsyncLazy {
        stats.onTaskLaunched("OutputTask", name)
        started = true
        try {
            val image = producer.join().getOrThrow()
            withContext(Dispatchers.IO.plus(CoroutineName(name))) {
                image.writePng(file)
            }
            if (!file.exists()) {
                return@StrongAsyncLazy failure(RuntimeException("OutputTask $name appeared to succeed, but $file still doesn't exist"))
            }
            completed = true
            stats.onTaskCompleted("OutputTask", name)
            return@StrongAsyncLazy success(Unit)

        } catch (e: NotImplementedError) {
            logger.warn("Skipping $name because it's not implemented yet")
            completed = true
            return@StrongAsyncLazy success(Unit)
        } catch (t: Throwable) {
            return@StrongAsyncLazy failure(t)
        }
    }
    override suspend fun run() = result.get().getOrThrow()
    override suspend fun join(): Result<Unit>  = result.get()

    override suspend fun clearResult() {
        result.clearResult()
        started = false
    }

    override fun isComplete(): Boolean = completed

    override fun isStarted(): Boolean = started

    override fun isFailed(): Boolean = result.getNow()?.isFailure == true

    override fun dependencies(): Collection<Task<PackedImage>> = listOf(producer)

    override fun toString(): String = "Output of $name"
    override fun formatTo(buffer: StringBuilder) {
        buffer.append("Output of ").append(name)
    }
}