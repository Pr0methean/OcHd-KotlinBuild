package io.github.pr0methean.ochd.tasks.consumable

import io.github.pr0methean.ochd.tasks.consumable.caching.TaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.util.Unbox
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

private val logger = LogManager.getLogger("AbstractConsumableTask")
private val cancelBecauseReplacing = CancellationException("Being replaced")
abstract class AbstractConsumableTask<T>(override val name: String, private val cache: TaskCache<T>) : ConsumableTask<T> {
    private val mutex = Mutex()
    protected val attemptNumber = AtomicLong()
    @Volatile
    protected var runningAttemptNumber = -1L

    class AttemptNumberCtx(val attempt: Long): CoroutineContext.Element {
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
    val coroutineHandle: AtomicReference<DisposableHandle?> = AtomicReference(null)
    private val consumers = ConcurrentHashMap.newKeySet<suspend (Result<T>) -> Unit>()
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class, InternalCoroutinesApi::class)
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

    @OptIn(DelicateCoroutinesApi::class, InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
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
        val resultAfterLock = mutex.withLock(newCoroutine) {
            startCoroutineWhileLockedAsync(newCoroutine, attemptNumber, scope)
        }
        logger.debug("Unlocking {} now that it has started", this)
        return resultAfterLock
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
            val oldHandle = coroutineHandle.getAndSet(newHandle)
            newCoroutine.start()
            logger.debug("Started {}", this)
            oldHandle?.dispose()
            return newCoroutine
        } else {
            logger.debug("Already started {}", this)
        }
        newCoroutine.cancel("Not started because a copy is already running")
        return oldCoroutine
    }

    private suspend fun createCoroutineAsync(): Deferred<Result<T>> {
        val attempt = attemptNumber.incrementAndGet()
        return createCoroutineAsync(createCoroutineScope(attempt))
    }

    protected abstract suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>>

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
        @Suppress("DeferredResultUnused") val oldConsumers = mutex.withLock(result) {
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
            val oldConsumers = mutableListOf<suspend (Result<T>) -> Unit>()
            consumers.forEach(oldConsumers::add)
            oldConsumers
        }
        logger.debug("Unlocking {} after emitting {}", this, result)
        logger.debug("Invoking {} consumers of {} with result {}", Unbox.box(oldConsumers.size), this, result)
        oldConsumers.asFlow().collect {
            logger.debug("Invoking {} (consumer of {}) with result {}", it, this, result)
            it(result)
            consumers.remove(it)
            logger.debug("Done running {} (consumer of {}) with result {}", it, this, result)
        }
    }

    override suspend fun await(): Result<T> {
        val resultNow = getNow()
        if (resultNow != null) {
            return resultNow
        }
        val resultRetriever = consumeAsync {it}
        logger.debug("Awaiting {} with consumers {}", this, consumers)
        return resultRetriever.await()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun clearFailure() {
        logger.debug("Locking {} to clear failure", this)
        mutex.withLock(this@AbstractConsumableTask) {
            if (getNow()?.isFailure == true) {
                logger.debug("Clearing failure from {}", this)
                runningAttemptNumber = -1
                set(null)
                val oldCoroutine = coroutine.getAndSet(null)
                if (oldCoroutine?.isCompleted == false) {
                    logger.debug("Canceling failed coroutine for {}", this)
                    oldCoroutine.cancel(cancelBecauseReplacing)
                }
            } else {
                logger.debug("No failure to clear for {}", this)
            }
        }
        logger.debug("Unlocking {} after clearing failure", this)
    }

    @Suppress("DeferredResultUnused")
    override suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R> {
        val resultNow = getNow()
        if (resultNow != null) {
            logger.debug("Consuming cached result immediately in consumeAsync")
            return CompletableDeferred(block(resultNow))
        }
        logger.debug("Locking {} for consumeAsync double check", this)
        var resultAfterLocking: Result<T>? = null
        var newDeferred: Deferred<R>? = null
        mutex.withLock(block) {
            val result = getNow()
            if (result != null) {
                logger.debug("Unlocking {} after consumeAsync double check (result found)", this)
                resultAfterLocking = result
            } else {
                val deferred = CompletableDeferred<R>()
                consumers.add {
                    deferred.complete(block(it))
                }
                logger.debug("Unlocking {} for consumeAsync after adding consumer; new set of consumers is {}", this, consumers)
                startWhileLockedAsync()
                newDeferred = deferred
            }
        }
        if (resultAfterLocking != null) {
            return CompletableDeferred(block(resultAfterLocking!!))
        }
        return newDeferred!!
    }

    override suspend fun checkSanity() {
        logger.debug("Locking {} for sanity check", this)
        mutex.withLock(this@AbstractConsumableTask) {
            if (consumers.isNotEmpty() && coroutine.get()?.isActive != true) {
                logger.error("{} has consumers {} waiting but is not running", this, consumers)
            }
        }
        logger.debug("Unlocking {} after sanity check", this)
    }

    protected open suspend fun createCoroutineScope(attempt: Long) = CoroutineScope(
        currentCoroutineContext()
            .plus(CoroutineName(name))
            .plus(AttemptNumberCtx(attempt))
            .plus(SupervisorJob())
    )

    @OptIn(DelicateCoroutinesApi::class)
    override suspend fun mergeWithDuplicate(other: ConsumableTask<T>): ConsumableTask<T> {
        if (other === this) {
            return this
        }
        if (getNow() != null) {
            return this
        }
        logger.debug("Locking {} to merge with a duplicate", this)
        mutex.withLock(this@AbstractConsumableTask) {
            if (other is AbstractConsumableTask) {
                logger.debug("Locking {} to merge list of consumers into {}", other, this)
                other.mutex.withLock(this@AbstractConsumableTask) {
                    other.consumers.forEach(consumers::add)
                }
                logger.debug("Unlocking {} after merging list of consumers into {}", other, this)
                if (getNow() != null) {
                    return@withLock
                }
                val result = other.getNow()
                if (result != null) {
                    createCoroutineScope(other.runningAttemptNumber).launch {emit(result)}
                    return@withLock
                }
                if (coroutine.get() == null) {
                    logger.debug("Locking {} to merge into {}", other, this)
                    other.mutex.withLock(this@AbstractConsumableTask) {
                        val resultWithLock = other.getNow()
                        runningAttemptNumber = other.runningAttemptNumber
                        if (resultWithLock != null) {
                            createCoroutineScope(other.runningAttemptNumber).launch {emit(resultWithLock)}
                            return this
                        }
                        attemptNumber.updateAndGet {max(it, other.attemptNumber.get())}
                        coroutine.set(other.coroutine.get())
                    }
                    logger.debug("Unlocking {} after merging it into {}", other, this)
                }
            }
        }
        logger.debug("Unlocking {} after merging a duplicate into it", this)
        return this
    }
}