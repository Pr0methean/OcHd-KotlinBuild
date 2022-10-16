package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.NoopTaskCache
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import java.util.Collections.newSetFromMap
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure

val abstractTaskLogger: Logger = LogManager.getLogger("AbstractTask")
private val cancelBecauseReplacing = CancellationException("Being replaced")
abstract class AbstractTask<T>(final override val name: String, val cache: TaskCache<T>) : Task<T> {
    val directDependentTasks: MutableSet<Task<*>> = newSetFromMap(WeakHashMap())
    val mutex: Mutex = Mutex()
    override fun addDirectDependentTask(task: Task<*>): Unit = synchronized(directDependentTasks)
    {
        directDependentTasks.add(task)
        if (directDependentTasks.size >= 2 && !cache.enabled && cache !is NoopTaskCache) {
            abstractTaskLogger.info("Enabling caching for {}", name)
            cache.enabled = true
        }
    }

    override fun removeDirectDependentTask(task: Task<*>): Unit = synchronized(directDependentTasks) {
        directDependentTasks.remove(task)
        if (directDependentTasks.isEmpty() && cache.enabled) {
            abstractTaskLogger.info("Disabling caching for {}", name)
            cache.enabled = false
        }
    }

    override val totalSubtasks: Int by lazy {
        var total = 0
        for (task in directDependencies) {
            total += 1 + task.totalSubtasks
        }
        total
    }

    override fun cachedSubtasks(): Int {
        if (getNow() != null) {
            return totalSubtasks + 1
        }
        var total = 0
        for (task in directDependencies) {
            total += if (task.getNow() != null) task.totalSubtasks + 1 else task.cachedSubtasks()
        }
        return total
    }

    override fun registerRecursiveDependencies() {
        directDependencies.forEach {
            it.addDirectDependentTask(this@AbstractTask)
            it.registerRecursiveDependencies()
        }
    }

    override fun isCachingEnabled(): Boolean = cache.enabled

    final override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @GuardedBy("mutex")
    val coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)
    @GuardedBy("mutex")
    val coroutineHandle: AtomicReference<DisposableHandle?> = AtomicReference(null)

    @OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
    override fun getNow(): Result<T>? {
        val cached = cache.getNow()
        if (cached != null) {
            abstractTaskLogger.debug("Retrieved {} from cache", cached)
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
            abstractTaskLogger.debug("Retrieved {} from coroutine in getNow", result)
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
        abstractTaskLogger.debug("Locking {} to start it", this)
        mutex.withLock {
            val resultWithLock = getNow()
            if (resultWithLock != null) {
                abstractTaskLogger.debug("Found result {} before we could start {}", resultWithLock, this)
                return CompletableDeferred(resultWithLock)
            }
            val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
            if (oldCoroutine != null) {
                abstractTaskLogger.debug("Already started {}", this)
                newCoroutine.cancel("Not started because a copy is already running")
                return oldCoroutine
            } else {
                abstractTaskLogger.debug("Starting {}", this)
                val newHandle = newCoroutine.invokeOnCompletion(onCancelling = true) {
                    if (it === cancelBecauseReplacing) {
                        return@invokeOnCompletion
                    }
                    if (it != null) {
                        abstractTaskLogger.error("Handling exception in invokeOnCompletion", it)
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
                abstractTaskLogger.debug("Started {}", this)
                oldHandle?.dispose()
                return newCoroutine
            }
        }
    }

    protected abstract suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>>

    @Suppress("DeferredResultUnused")
    suspend inline fun emit(result: Result<T>, source: Deferred<Result<T>>?) {
        abstractTaskLogger.debug("Locking {} to emit {}", this, result)
        synchronized (directDependentTasks) {
            if (cache.enabled && directDependentTasks.size < 2) {
                abstractTaskLogger.info("Disabling caching for {} while emitting result", this)
                cache.enabled = false
            }
        }
        cache.set(result)
        mutex.withLock(result) {
            if (coroutine.compareAndSet(source, null)) {
                coroutineHandle.getAndSet(null)?.dispose()
            }
        }
        abstractTaskLogger.debug("Unlocking {} after emitting result", this)
    }

    override suspend fun clearFailure() {
        abstractTaskLogger.debug("Locking {} to clear failure", this)
        if (mutex.tryLock(this@AbstractTask)) {
            try {
                if (getNow()?.isFailure == true) {
                    abstractTaskLogger.debug("Clearing failure from {}", this)
                    cache.set(null)
                    val oldCoroutine = coroutine.getAndSet(null)
                    if (oldCoroutine?.isCompleted == false) {
                        abstractTaskLogger.debug("Canceling failed coroutine for {}", this)
                        oldCoroutine.cancel(cancelBecauseReplacing)
                    }
                    coroutineHandle.getAndSet(null)?.dispose()
                } else {
                    abstractTaskLogger.debug("No failure to clear for {}", this)
                }
            } finally {
                abstractTaskLogger.debug("Unlocking {} after clearing failure", this)
                mutex.unlock(this@AbstractTask)
            }
        } else {
            abstractTaskLogger.warn("Couldn't acquire lock for {}.clearFailure()", this)
        }
    }

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
        abstractTaskLogger.debug("Locking {} to merge with a duplicate", this)
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
                abstractTaskLogger.debug("Locking {} to merge into {}", other, this)
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
                abstractTaskLogger.debug("Unlocking {} after merging it into {}", other, this)
            }
        }
        abstractTaskLogger.debug("Unlocking {} after merging a duplicate into it", this)
        return this
    }

    private val supervisorJob = SupervisorJob()
    private val coroutineName = CoroutineName(name)

    override suspend fun createCoroutineScope(): CoroutineScope = CoroutineScope(
        currentCoroutineContext()
            .plus(coroutineName)
            .plus(supervisorJob)
    )

    override fun isStartedOrAvailable(): Boolean = coroutine.get()?.run { isActive || isCompleted } == true
            || getNow() != null
}