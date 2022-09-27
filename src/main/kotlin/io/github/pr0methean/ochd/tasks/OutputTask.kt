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
    val source: PngCompressionTask,
    override val name: String,
    val stats: ImageProcessingStats,
    private val files: List<File>,
): TransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache(), transform = { bytes ->
    withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        stats.onTaskLaunched("OutputTask", name)
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
}) {
    init {
        if (files.isEmpty()) {
            throw IllegalArgumentException("OutputTask with no destination files")
        }
    }
}