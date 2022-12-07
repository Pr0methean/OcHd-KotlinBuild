package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.noopTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files

@Suppress("BlockingMethodInNonBlockingContext")
class OutputTask(
    val source: Task<ByteArray>,
    name: String,
    val stats: ImageProcessingStats,
    private var files: List<File>,
): TransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache()) {
    override suspend fun transform(input: ByteArray) {
        stats.onTaskLaunched("OutputTask", name)
        withContext(Dispatchers.IO.plus(CoroutineName(name))) {
            val firstFile = files[0]
            firstFile.parentFile?.mkdirs()
            Files.write(firstFile.toPath(), input)
            val firstFilePath = firstFile.absoluteFile.toPath()
            for (file in files.subList(1, files.size)) {
                file.parentFile?.mkdirs()
                Files.copy(firstFilePath, file.absoluteFile.toPath())
            }
            stats.onTaskCompleted("OutputTask", name)
        }
    }

    val isCommandBlock: Boolean = name.contains("command_block")

    init {
        check(files.isNotEmpty()) { "OutputTask $name has no destination files" }
    }
}
