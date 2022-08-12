package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

private val logger = LogManager.getLogger("AbstractTask")
private val cancelBecauseReplacing = CancellationException("Being replaced")
abstract class AbstractTask<T>(override val name: String, private val cache: TaskCache<T>) : Task<T> {
    private val mutex = Mutex()
    protected val attemptNumber: AtomicLong = AtomicLong()
    @Volatile
    protected var runningAttemptNumber: Long = -1L

    data class AttemptNumberCtx(val attempt: Long): CoroutineContext.Element {
        override val key = AttemptNumberKey
    }

    object AttemptNumberKey: CoroutineContext.Key<AttemptNumberCtx>
    private val toString by lazy {
        StringBuilder().apply(::formatTo).toString()
    }
    final override fun toString(): String = toString

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    val coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)
    private val coroutineHandle: AtomicReference<DisposableHandle?> = AtomicReference(null)

    @OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
    override fun getNow(): Result<T>? {
        val cached = cache.getNow()
        if (cached != null) {
            logger.debug("Retrieved {} from cache", cached)
            return cached
        }
        val coroutine = coroutine.get()
        val result = if (coroutine?.isCompleted == true) {
            try {
                coroutine.getCompleted()
            } catch (t: Throwable) {
                failure(t)
            }
        } else if (coroutine?.isCancelled == true) {
            failure(coroutine.getCancellationException())
        } else null
        if (result != null) {
            logger.debug("Retrieved {} from coroutine in getNow", result)
        }
        return result
    }
    private fun set(value: Result<T>?) = cache.set(value)

    protected open suspend fun startPrerequisites() {}

    override suspend fun startAsync(): Deferred<Result<T>> {
        val result = getNow()
        if (result != null) {
            return CompletableDeferred(result)
        }
        startPrerequisites()
        val attemptNumber = attemptNumber.incrementAndGet()
        val scope = createCoroutineScope(attemptNumber)
        val newCoroutine = createCoroutineAsync(scope)
        logger.debug("Locking {} to start it", this)
        val resultDeferred = mutex.withLock(newCoroutine) {
            startCoroutineWhileLockedAsync(newCoroutine, attemptNumber, scope)
        }
        logger.debug("Unlocking {} now that it has started", this)
        return resultDeferred
    }

    @GuardedBy("mutex")
    private suspend fun startWhileLockedAsync(): Deferred<Result<T>> {
        val result = getNow()
        if (result != null) {
            return CompletableDeferred(result)
        }
        startPrerequisites()
        val attemptNumber = attemptNumber.incrementAndGet()
        val scope = createCoroutineScope(attemptNumber)
        val newCoroutine = createCoroutineAsync(scope)
        return startCoroutineWhileLockedAsync(newCoroutine, attemptNumber, scope)
    }

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    @GuardedBy("mutex")
    private fun startCoroutineWhileLockedAsync(
        newCoroutine: Deferred<Result<T>>,
        attemptNumber: Long,
        scope: CoroutineScope
    ): Deferred<Result<T>> {
        val resultWithLock = getNow()
        if (resultWithLock != null) {
            logger.debug("Found result {} before we could start {}", resultWithLock, this)
            return CompletableDeferred(resultWithLock)
        }
        val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
        if (oldCoroutine == null) {
            logger.debug("Starting {}", this)
            runningAttemptNumber = attemptNumber
            val newHandle = newCoroutine.invokeOnCompletion(onCancelling = true) {
                coroutineHandle.set(null)
                if (it === cancelBecauseReplacing) {
                    return@invokeOnCompletion
                }
                if (it != null) {
                    logger.error("Handling exception in invokeOnCompletion", it)
                    scope.launch { emit(failure(it)) }
                } else {
                    scope.launch { emit(newCoroutine.getCompleted()) }
                }
            }
            newCoroutine.start()
            val oldHandle = coroutineHandle.getAndSet(newHandle)
            logger.debug("Started {}", this)
            oldHandle?.dispose()
            return newCoroutine
        } else {
            logger.debug("Already started {}", this)
            newCoroutine.cancel("Not started because a copy is already running")
            return oldCoroutine
        }
    }

    private suspend fun createCoroutineAsync(): Deferred<Result<T>> {
        val attempt = attemptNumber.incrementAndGet()
        return createCoroutineAsync(createCoroutineScope(attempt))
    }

    protected abstract suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>>

    @Suppress("DeferredResultUnused")
    suspend fun emit(result: Result<T>) {
        val thisAttempt = currentCoroutineContext()[AttemptNumberKey]?.attempt ?: -2
        if (result.isFailure && runningAttemptNumber != thisAttempt && thisAttempt != -2L) {
            logger.debug(
                "Wrong attempt number (expected {}, was {}) for result {}",
                thisAttempt, runningAttemptNumber, result
            )
            return
        }
        logger.debug("Locking {} to emit {}", this, result)
        mutex.withLock(result) {
            if (result.isFailure && runningAttemptNumber != thisAttempt && thisAttempt != -2L) {
                logger.debug("Unlocking {}: Wrong attempt number (expected {}, was {}) for result {}",
                    this, thisAttempt, runningAttemptNumber, result)
                return
            } else {
                logger.debug("Emitting result {} from attempt {}", result, thisAttempt)
            }
            set(result)
            runningAttemptNumber = -1
            coroutine.getAndSet(null)
            coroutineHandle.get()?.dispose()
        }
    }

    override suspend fun await(): Result<T> = startAsync().await()

    override suspend fun clearFailure() {
        logger.debug("Locking {} to clear failure", this)
        mutex.withLock(this@AbstractTask) {
            if (getNow()?.isFailure == true) {
                logger.debug("Clearing failure from {}", this)
                runningAttemptNumber = -1
                set(null)
                val oldCoroutine = coroutine.getAndSet(null)
                if (oldCoroutine?.isCompleted == false) {
                    logger.debug("Canceling failed coroutine for {}", this)
                    oldCoroutine.cancel(cancelBecauseReplacing)
                }
                coroutineHandle.getAndSet(null)?.dispose()
            } else {
                logger.debug("No failure to clear for {}", this)
            }
        }
        logger.debug("Unlocking {} after clearing failure", this)
    }

    override fun enableCaching() = cache.enable()

    @Suppress("DeferredResultUnused")
    override suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R>
            = createCoroutineScope(-1).async {
                block(startAsync().await())
            }

    protected open suspend fun createCoroutineScope(attempt: Long): CoroutineScope = CoroutineScope(
        currentCoroutineContext()
            .plus(CoroutineName(name))
            .plus(AttemptNumberCtx(attempt))
            .plus(SupervisorJob())
    )

    override suspend fun mergeWithDuplicate(other: Task<T>): Task<T> {
        if (other === this) {
            return this
        }
        if (getNow() != null) {
            return this
        }
        logger.debug("Locking {} to merge with a duplicate", this)
        if (other is AbstractTask) {
            mutex.withLock(this@AbstractTask) {
                if (getNow() != null) {
                    return@withLock
                }
                val result = other.getNow()
                if (result != null) {
                    emit(result)
                    return this
                }
                if (coroutine.get() == null) {
                    logger.debug("Locking {} to merge into {}", other, this)
                    other.mutex.withLock(this@AbstractTask) {
                        val resultWithLock = other.getNow()
                        if (resultWithLock != null) {
                            emit(resultWithLock)
                            return this
                        }
                        val otherCoroutine = other.coroutine.get()
                        if (otherCoroutine != null) {
                            runningAttemptNumber = other.runningAttemptNumber
                            attemptNumber.updateAndGet { max(it, other.attemptNumber.get()) }
                            coroutine.set(createCoroutineScope(runningAttemptNumber).async {otherCoroutine.await()})
                            return this
                        }
                    }
                    logger.debug("Unlocking {} after merging it into {}", other, this)
                }
            }
        }
        logger.debug("Unlocking {} after merging a duplicate into it", this)
        return this
    }
}