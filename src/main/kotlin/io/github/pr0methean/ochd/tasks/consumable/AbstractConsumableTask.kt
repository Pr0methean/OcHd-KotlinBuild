package io.github.pr0methean.ochd.tasks.consumable

import com.google.common.collect.MapMaker
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicReference

private val logger = LogManager.getLogger("AbstractConsumableTask")
abstract class AbstractConsumableTask<T>(override val name: String, private val cache: TaskCache<T>) : ConsumableTask<T> {
    val mutex = Mutex()
    override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @Volatile
    var coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)
    val consumers: MutableSet<suspend (Result<T>) -> Unit> = Collections.newSetFromMap(MapMaker().weakKeys().makeMap())
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun getNow(): Result<T>? {
        val cached = cache.getNow()
        if (cached != null) {
            return cached
        }
        val coroutine = coroutine.get()
        if (coroutine?.isCompleted == true) {
            val result = coroutine.getCompleted()
            set(result)
            return result
        }
        return null
    }
    protected fun set(value: Result<T>?) = cache.set(value)

    override suspend fun startAsync(): Deferred<Result<T>> {
        val newCoroutine = createCoroutineAsync()
        val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
        if (oldCoroutine == null) {
            newCoroutine.start()
            return newCoroutine
        }
        return oldCoroutine
    }

    protected abstract suspend fun createCoroutineAsync(): Deferred<Result<T>>

    suspend fun emit(result: Result<T>) {
        set(result)
        consumers.forEach { it(result) }
    }

    override suspend fun await(): Result<T> {
        val resultNow = getNow()
        if (resultNow != null) {
            return resultNow
        }
        val coroutine = mutex.withLock {
            val resultAfterLocking = getNow()
            if (resultAfterLocking != null) {
                return resultAfterLocking
            }
            startAsync()
        }
        return coroutine.await()
    }

    override suspend fun clearFailure() {
        mutex.withLock {
            if (getNow()?.isFailure == true) {
                set(null)
                coroutine.set(null)
            }
        }
    }

    override suspend fun consume(block: suspend (Result<T>) -> Unit) {
        val resultNow = getNow()
        if (resultNow != null) {
            return block(resultNow)
        }
        mutex.withLock {
            val resultAfterLocking = getNow()
            if (resultAfterLocking != null) {
                return block(resultAfterLocking)
            }
            consumers.add(block)
            startAsync()
        }
    }
}