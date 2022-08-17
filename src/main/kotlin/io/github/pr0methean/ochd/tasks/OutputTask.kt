package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files

private val logger = LogManager.getLogger("OutputTask")
@Suppress("BlockingMethodInNonBlockingContext")
class OutputTask(
    source: Task<ByteArray>,
    override val name: String,
    val stats: ImageProcessingStats,
    private val files: List<File>,
): AsyncTransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache(), transform = { bytes ->
    withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        stats.onTaskLaunched("OutputTask", name)
        if (files.isEmpty()) {
            logger.warn("OutputTask $name has no files to write to!")
            return@withContext
        }
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
    }
    stats.onTaskCompleted("OutputTask", name)
})