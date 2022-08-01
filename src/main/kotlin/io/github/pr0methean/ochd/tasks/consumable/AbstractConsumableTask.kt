package io.github.pr0methean.ochd.tasks.consumable

import com.google.common.collect.MapMaker
import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure
import kotlin.coroutines.CoroutineContext

private val logger = LogManager.getLogger("AbstractConsumableTask")
abstract class AbstractConsumableTask<T>(override val name: String, private val cache: TaskCache<T>) : ConsumableTask<T> {
    private val mutex = Mutex()
    @Suppress("LeakingThis")
    protected val exceptionHandler = ExceptionHandler(this)
    protected val attemptNumber = AtomicLong()
    @Volatile
    protected var runningAttemptNumber = -1L
    class ExceptionHandler(val task: AbstractConsumableTask<*>) : CoroutineExceptionHandler {
        override val key: CoroutineContext.Key<*> = CoroutineExceptionHandler.Key
        override fun handleException(context: CoroutineContext, exception: Throwable) {
            val thisAttempt = context[AttemptNumberKey]?.attempt ?: -2
            CoroutineScope(context.plus(SupervisorJob())).launch {
                task.mutex.withLock {
                    if (task.runningAttemptNumber == thisAttempt) {
                        task.emit(failure(exception))
                    }
                }
            }
        }
    }

    class AttemptNumberCtx(val attempt: Long): CoroutineContext.Element {
        override val key = AttemptNumberKey
    }

    object AttemptNumberKey: CoroutineContext.Key<AttemptNumberCtx>

    final override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    val coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)
    val consumers: MutableSet<suspend (Result<T>) -> Unit> = Collections.newSetFromMap(MapMaker().weakKeys().makeMap())
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    override fun getNow(): Result<T>? {
        val cached = cache.getNow()
        if (cached != null) {
            logger.debug("Retrieved {} from cache", cached)
            return cached
        }
        val coroutine = coroutine.get()
        if (coroutine?.isCompleted == true) {
            val result = coroutine.getCompleted()
            logger.debug("Retrieved {} from coroutine in getNow", result)
            GlobalScope.launch {emit(result)}
            return result
        }
        return null
    }
    private fun set(value: Result<T>?) = cache.set(value)

    @GuardedBy("mutex")
    override suspend fun startAsync(): Deferred<Result<T>> {
        val newCoroutine = createCoroutineAsync()
        val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
        if (oldCoroutine == null) {
            logger.debug("Starting {}", name)
            runningAttemptNumber = attemptNumber.get()
            newCoroutine.start()
            return newCoroutine
        } else {
            logger.debug("Already started: {}", name)
        }
        return oldCoroutine
    }

    protected abstract suspend fun createCoroutineAsync(): Deferred<Result<T>>

    suspend fun emit(result: Result<T>) {
        val (oldCoroutine, oldConsumers) = mutex.withLock {
            set(result)
            runningAttemptNumber = -1
            val oldCoroutine = coroutine.getAndSet(null)
            val oldConsumers = consumers.toList()
            consumers.clear()
            oldCoroutine to oldConsumers
        }
        if (oldCoroutine?.isCompleted == false) {
            oldCoroutine.cancel()
        }
        oldConsumers.forEach { it(result) }
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
        logger.debug("Awaiting {}", name)
        return coroutine.await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun clearFailure() {
        logger.debug("Waiting for mutex so we can clear failure from {}", name)
        mutex.withLock {
            if (getNow()?.isFailure == true || coroutine.get()?.isCancelled == true || coroutine.get()?.getCompleted()?.isFailure == true) {
                logger.debug("Clearing failure from {}", name)
                set(null)
                coroutine.set(null)
                runningAttemptNumber = -1
            } else {
                logger.debug("No failure to clear for {}", name)
            }
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R> {
        val resultNow = getNow()
        if (resultNow != null) {
            return CompletableDeferred(block(resultNow))
        }
        val resultAfterLocking = mutex.withLock {
            val result = getNow()
            if (result == null) {
                val deferred = CompletableDeferred<R>()
                consumers.add {
                    deferred.complete(block(it))
                }
                startAsync()
                return deferred
            }
            result
        }
        return CompletableDeferred(block(resultAfterLocking))
    }

    override suspend fun checkSanity() {
        mutex.withLock {
            if (consumers.isNotEmpty() && coroutine.get()?.isActive != true) {
                logger.error("{} has consumers {} waiting but is not running", name, consumers)
            }
        }
    }

    protected open suspend fun createCoroutineScope(attempt: Long) = CoroutineScope(
        currentCoroutineContext()
            .plus(CoroutineName(name))
            .plus(AttemptNumberCtx(attempt))
            .plus(exceptionHandler)
            .plus(SupervisorJob())
    )

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun mergeWithDuplicate(other: ConsumableTask<T>): ConsumableTask<T> {
        if (getNow() != null) {
            return this
        }
        mutex.withLock {
            if (getNow() != null) {
                return@withLock
            }
            val result = other.getNow()
            if (result != null) {
                emit(result)
                return@withLock
            }
            if (coroutine.get() == null && other is AbstractConsumableTask) {
                other.mutex.withLock {
                    val resultWithLock = other.getNow()
                    if (resultWithLock != null) {
                        GlobalScope.launch {emit(resultWithLock)}
                        return this
                    }
                    coroutine.set(other.coroutine.get())
                }
            }
        }
        return this
    }
}