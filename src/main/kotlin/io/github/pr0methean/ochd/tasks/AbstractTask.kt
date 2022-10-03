package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.NoopTaskCache
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.*
import java.util.Collections.newSetFromMap
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure

private val logger = LogManager.getLogger("AbstractTask")
private val cancelBecauseReplacing = CancellationException("Being replaced")
abstract class AbstractTask<T>(override val name: String, internal val cache: TaskCache<T>) : Task<T> {
    private val directDependentTasks: MutableSet<Task<*>> = newSetFromMap(WeakHashMap())
    private val mutex = Mutex()
    override fun addDirectDependentTask(task: Task<*>): Unit = synchronized(directDependentTasks)
    {
        directDependentTasks.add(task)
        if (directDependentTasks.size >= 2 && !cache.enabled && cache !is NoopTaskCache) {
            logger.info("Enabling caching for {}", name)
            cache.enabled = true
        }
    }

    override fun removeDirectDependentTask(task: Task<*>): Unit = synchronized(directDependentTasks) {
        directDependentTasks.remove(task)
        if (directDependentTasks.isEmpty() && cache.enabled) {
            logger.info("Disabling caching for {}", name)
            cache.enabled = false
        }
    }

    final override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @GuardedBy("mutex")
    val coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)
    @GuardedBy("mutex")
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

    @OptIn(InternalCoroutinesApi::class, ExperimentalCoroutinesApi::class)
    override suspend fun startAsync(): Deferred<Result<T>> {
        val result = getNow()
        if (result != null) {
            return CompletableDeferred(result)
        }
        val maybeAlreadyStarted = coroutine.get()
        if (maybeAlreadyStarted != null) {
            return maybeAlreadyStarted
        }
        val scope = createCoroutineScope()
        val newCoroutine = createCoroutineAsync(scope)
        logger.debug("Locking {} to start it", this)
        mutex.withLock {
            val resultWithLock = getNow()
            if (resultWithLock != null) {
                logger.debug("Found result {} before we could start {}", resultWithLock, this)
                return CompletableDeferred(resultWithLock)
            }
            val maybeAlreadyStartedWithLock = coroutine.get()
            if (maybeAlreadyStartedWithLock != null) {
                logger.debug("Found coroutine {} before we could start {}", maybeAlreadyStartedWithLock, this)
                return maybeAlreadyStartedWithLock
            }
            val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
            if (oldCoroutine != null) {
                logger.debug("Already started {}", this)
                newCoroutine.cancel("Not started because a copy is already running")
                return oldCoroutine
            } else {
                logger.debug("Starting {}", this)
                val newHandle = newCoroutine.invokeOnCompletion(onCancelling = true) {
                    if (it === cancelBecauseReplacing) {
                        return@invokeOnCompletion
                    }
                    if (it != null) {
                        logger.error("Handling exception in invokeOnCompletion", it)
                        scope.launch { emit(failure(it), newCoroutine) }
                    } else {
                        scope.launch { emit(newCoroutine.getCompleted(), newCoroutine) }
                    }
                    runBlocking {
                        mutex.withLock {
                            coroutineHandle.set(null)
                        }
                    }
                }
                newCoroutine.start()
                val oldHandle = coroutineHandle.getAndSet(newHandle)
                logger.debug("Started {}", this)
                oldHandle?.dispose()
                return newCoroutine
            }
        }
    }

    protected abstract suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>>

    @Suppress("DeferredResultUnused")
    suspend fun emit(result: Result<T>, source: Deferred<Result<T>>?) {
        logger.debug("Locking {} to emit {}", this, result)
        synchronized (directDependentTasks) {
            if (cache.enabled && directDependentTasks.size < 2) {
                logger.info("Disabling caching for {} while emitting result", this)
                cache.enabled = false
            }
        }
        cache.set(result)
        mutex.withLock(result) {
            if (coroutine.compareAndSet(source, null)) {
                coroutineHandle.getAndSet(null)?.dispose()
            }
        }
        logger.debug("Unlocking {} after emitting result", this)
    }

    override suspend fun await(): Result<T> = startAsync().await()

    override suspend fun clearFailure() {
        logger.debug("Locking {} to clear failure", this)
        if (mutex.tryLock(this@AbstractTask)) {
            try {
                if (getNow()?.isFailure == true) {
                    logger.debug("Clearing failure from {}", this)
                    cache.set(null)
                    val oldCoroutine = coroutine.getAndSet(null)
                    if (oldCoroutine?.isCompleted == false) {
                        logger.debug("Canceling failed coroutine for {}", this)
                        oldCoroutine.cancel(cancelBecauseReplacing)
                    }
                    coroutineHandle.getAndSet(null)?.dispose()
                } else {
                    logger.debug("No failure to clear for {}", this)
                }
            } finally {
                logger.debug("Unlocking {} after clearing failure", this)
                mutex.unlock(this@AbstractTask)
            }
        } else {
            logger.warn("Couldn't acquire lock for {}.clearFailure()", this)
        }
    }

    @Suppress("DeferredResultUnused")
    override suspend fun <R> consumeAsync(block: suspend (Result<T>) -> R): Deferred<R>
            = createCoroutineScope().async {
                block(await())
            }

    protected open suspend fun createCoroutineScope(): CoroutineScope = CoroutineScope(
        currentCoroutineContext()
            .plus(CoroutineName(name))
            .plus(SupervisorJob())
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun mergeWithDuplicate(other: Task<T>): Task<T> {
        if (other === this) {
            return this
        }
        if (getNow() != null) {
            return this
        }
        val otherNow = other.getNow()
        if (otherNow != null) {
            emit(otherNow, (other as? AbstractTask)?.coroutine?.get())
            return this
        }
        logger.debug("Locking {} to merge with a duplicate", this)
        mutex.withLock(other) {
            if (getNow() != null) {
                return@withLock
            }
            val result = other.getNow()
            if (result != null) {
                emit(result, (other as? AbstractTask)?.coroutine?.get())
                return this
            }
            if (coroutine.get() == null && other is AbstractTask) {
                logger.debug("Locking {} to merge into {}", other, this)
                other.mutex.withLock(this@AbstractTask) {
                    val resultWithLock = other.getNow()
                    if (resultWithLock != null) {
                        emit(resultWithLock, other.coroutine.get())
                        return this
                    }
                    val otherCoroutine = other.coroutine.get()
                    if (otherCoroutine != null) {
                        if (otherCoroutine.isCompleted) {
                            emit(otherCoroutine.getCompleted(), otherCoroutine)
                        } else {
                            coroutine.set(createCoroutineScope().async { otherCoroutine.await() })
                        }
                        return this
                    }
                }
                logger.debug("Unlocking {} after merging it into {}", other, this)
            }
        }
        logger.debug("Unlocking {} after merging a duplicate into it", this)
        return this
    }

}