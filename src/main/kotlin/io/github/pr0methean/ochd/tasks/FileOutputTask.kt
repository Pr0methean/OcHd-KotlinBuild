package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopDeferredTaskCache
import java.io.File
import java.nio.file.Files
import kotlin.coroutines.CoroutineContext

/**
 * Task that saves a [ByteArray] to one or more files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class FileOutputTask(
    name: String,
    source: Task<ByteArray>,
    private val files: List<File>,
    ctx: CoroutineContext,
    val stats: ImageProcessingStats,
): TransformingTask<ByteArray, Unit>("Output $name", source, noopDeferredTaskCache(), ctx) {
    override suspend fun transform(input: ByteArray) {
        stats.onTaskLaunched("FileOutputTask", name)
        val firstFile = files[0]
        firstFile.parentFile?.mkdirs()
        val firstFilePath = firstFile.absoluteFile.toPath()
        Files.write(firstFilePath, input)
        if (files.size > 1) {
            for (file in files.subList(1, files.size)) {
                file.parentFile?.mkdirs()
                Files.copy(firstFilePath, file.absoluteFile.toPath())
            }
        }
        stats.onTaskCompleted("FileOutputTask", name)
    }

    fun clearCache() {
        cache.clear()
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "FileOutputTask $name has no destination files" }
    }
}
