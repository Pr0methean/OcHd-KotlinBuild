package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.consumable.caching.StrongTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

@Suppress("BlockingMethodInNonBlockingContext")
data class OutputConsumableTask(
    val source: ConsumableTask<ByteArray>,
    override val name: String,
    private val file: File,
    val stats: ImageProcessingStats,
): SlowTransformingConsumableTask<ByteArray, Unit>(source, StrongTaskCache(), transform = { bytes ->
    withContext(Dispatchers.IO.plus(CoroutineName(name))) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { it.write(bytes) }
    }
    if (!file.exists()) {
        throw RuntimeException("OutputTask $name appeared to succeed, but $file still doesn't exist")
    }
    stats.onTaskCompleted("OutputTask", name)
})