package io.github.pr0methean.ochd

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

data class ReentrantSemaphoreContextKey(
    val semaphore: Semaphore
) : CoroutineContext.Key<ReentrantSemaphoreContextElement>

// From https://github.com/Kotlin/kotlinx.coroutines/issues/1686#issuecomment-777357672
suspend fun <T> Semaphore.withReentrantPermit(block: suspend () -> T): T {
    val key = ReentrantSemaphoreContextKey(this)
    // call block directly when this mutex is already locked in the context
    if (currentCoroutineContext()[key] != null) return block()
    // otherwise add it to the context and lock the mutex
    return withContext(ReentrantSemaphoreContextElement(key)) {
        withPermit { block() }
    }
}