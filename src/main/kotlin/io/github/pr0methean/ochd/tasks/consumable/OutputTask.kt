package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.noopTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
class OutputTask(
    source: Task<ByteArray>,
    override val name: String,
    val stats: ImageProcessingStats,
    private val files: List<File>,
): SlowTransformingTask<ByteArray, Unit>("Output $name", source, noopTaskCache(), transform = { bytes ->
    withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        stats.onTaskLaunched("OutputTask", name)
        for (file in files) {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(bytes) }
            if (!file.exists()) {
                throw RuntimeException("OutputTask $this appeared to succeed, but $file still doesn't exist")
            }
        }
    }
    stats.onTaskCompleted("OutputTask", name)
})