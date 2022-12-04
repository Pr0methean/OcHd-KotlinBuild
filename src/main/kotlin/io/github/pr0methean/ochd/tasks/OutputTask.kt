package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

@Suppress("BlockingMethodInNonBlockingContext")
class OutputTask(
    val source: Task<ByteArray>,
    name: String,
    val stats: ImageProcessingStats,
    private val files: List<File>,
): TransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache(), transform = { bytes ->
    stats.onTaskLaunched("OutputTask", name)
    withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        val firstFile = files[0]
        firstFile.parentFile?.mkdirs()
        FileOutputStream(firstFile).use {it.write(bytes)}
        if (!firstFile.exists()) {
            throw RuntimeException("OutputTask $name appeared to succeed, but $firstFile still doesn't exist")
        }
        val firstFilePath = firstFile.absoluteFile.toPath()
        for (file in files.subList(1, files.size)) {
            file.parentFile?.mkdirs()
            Files.copy(firstFilePath, file.absoluteFile.toPath())
            if (!file.exists()) {
                throw RuntimeException("OutputTask $name appeared to succeed, but $file still doesn't exist")
            }
        }
        stats.onTaskCompleted("OutputTask", name)
    }
}) {
    val isCommandBlock: Boolean = name.contains("command_block")
    init {
        if (files.isEmpty()) {
            throw IllegalArgumentException("OutputTask with no destination files")
        }
    }

    override fun hashCode(): Int = source.hashCode() + 127

    override suspend fun mergeWithDuplicate(other: Task<*>): OutputTask {
        if (other is OutputTask && source == other.source) {
            return OutputTask(source.mergeWithDuplicate(other.source), name, stats, files + other.files)
        }
        return super.mergeWithDuplicate(other) as OutputTask
    }

    override fun equals(other: Any?): Boolean = (this === other) || (other is OutputTask && source == other.source)
}