package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import java.io.File

abstract class OutputTask(private val scope: CoroutineScope, open val file: File) {
    private val coroutine = scope.async {invoke()}
    abstract suspend fun invoke()
    suspend fun await() = coroutine.await()
}