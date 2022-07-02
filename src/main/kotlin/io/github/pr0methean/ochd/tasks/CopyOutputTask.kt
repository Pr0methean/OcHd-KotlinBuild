package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import java.io.File

class CopyOutputTask(private val baseTask: OutputTask, override val file: File, scope: CoroutineScope)
        : OutputTask(scope, file) {
    override suspend fun invoke() {
        baseTask.invoke()
        baseTask.file.copyTo(file)
    }
}