package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import java.io.File
import java.nio.file.Files

/**
 * Task that saves a [ByteArray] to one or more files.
 */
@Suppress("BlockingMethodInNonBlockingContext")
class FileOutputTask(
    source: Task<ByteArray>,
    name: String,
    val stats: ImageProcessingStats,
    private val files: List<File>,
): TransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache()) {
    override suspend fun transform(input: ByteArray) {
        stats.onTaskLaunched("OutputTask", name)
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
        stats.onTaskCompleted("OutputTask", name)
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "OutputTask $name has no destination files" }
    }
}
