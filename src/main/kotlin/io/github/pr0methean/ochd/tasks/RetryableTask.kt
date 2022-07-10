package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import kotlinx.coroutines.Deferred
import java.util.concurrent.ExecutionException

abstract class RetryableTask<T>(open val ctx: ImageProcessingContext) {
    @Volatile
    protected var coroutine: Deferred<T>? = null

    suspend fun await(): T {
        var attempts = 1L
        var result: T? = null
        while (result == null) {
            var curCoroutine = coroutine
            if (curCoroutine == null || curCoroutine.isCancelled) {
                synchronized(this) {
                    curCoroutine = coroutine
                    if (curCoroutine == null || curCoroutine!!.isCancelled) {
                        curCoroutine = createCoroutineAsync()
                        coroutine = curCoroutine
                    }
                }
            }
            try {
                result = curCoroutine!!.await()
            } catch (t: Throwable) {
                if (!ctx.shouldRetry(t, attempts)) {
                    throw ExecutionException(t)
                } else {
                    attempts++
                }
            }
        }
        return result
    }

    abstract fun createCoroutineAsync(): Deferred<T>
}