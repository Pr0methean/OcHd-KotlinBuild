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
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure

val AT_LOGGER: Logger = LogManager.getLogger("AbstractTask")
private val CANCEL_BECAUSE_REPLACING = CancellationException("Being replaced")
private val SUPERVISOR_JOB = SupervisorJob()
abstract class AbstractTask<T>(final override val name: String, val cache: TaskCache<T>) : Task<T> {
    val timesFailed: AtomicLong = AtomicLong(0)
    val mutex: Mutex = Mutex()
    @GuardedBy("mutex")
    val directDependentTasks: MutableSet<Task<*>> = newSetFromMap(WeakHashMap())
    override suspend fun addDirectDependentTask(task: Task<*>): Unit = mutex.withLock {
        directDependentTasks.add(task)
        if (directDependentTasks.size >= 2 && !cache.enabled && cache !is NoopTaskCache) {
            AT_LOGGER.info("Enabling caching for {}", name)
            cache.enabled = true
        }
    }

    override suspend fun removeDirectDependentTask(task: Task<*>): Unit = mutex.withLock {
        directDependentTasks.remove(task)
        if (directDependentTasks.isEmpty() && cache.enabled) {
            AT_LOGGER.info("Disabling caching for {}", name)
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

    override fun cachedSubtasks(): Set<Task<*>> {
        if (getNow() != null) {
            return andAllSubtasks
        }
        if (directDependencies.none()) {
            return setOf()
        }
        val subtasks = mutableSetOf<Task<*>>()
        for (task in directDependencies) {
            subtasks += task.cachedSubtasks()
        }
        return subtasks
    }

    override val andAllSubtasks: Set<Task<*>> by lazy {
        val subtasks = mutableSetOf<Task<*>>(this)
        for (task in directDependencies) {
            subtasks += task.andAllSubtasks
        }
        subtasks
    }

    override suspend fun registerRecursiveDependencies(): Unit = mutex.withLock {
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
            AT_LOGGER.debug("Retrieved {} from cache", cached)
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
            AT_LOGGER.debug("Retrieved {} from coroutine in getNow", result)
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
        AT_LOGGER.debug("Locking {} to start it", name)
        mutex.withLock {
            val resultWithLock = getNow()
            if (resultWithLock != null) {
                AT_LOGGER.debug("Found result {} before we could start {}", resultWithLock, name)
                return CompletableDeferred(resultWithLock)
            }
            val oldCoroutine = coroutine.compareAndExchange(null, newCoroutine)
            if (oldCoroutine != null) {
                AT_LOGGER.debug("Already started {}", name)
                newCoroutine.cancel("Not started because a copy is already running")
                return oldCoroutine
            } else {
                AT_LOGGER.debug("Starting {}", name)
                val newHandle = newCoroutine.invokeOnCompletion(onCancelling = true) {
                    if (it === CANCEL_BECAUSE_REPLACING) {
                        return@invokeOnCompletion
                    }
                    if (it != null) {
                        AT_LOGGER.error("Handling exception in invokeOnCompletion", it)
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
                AT_LOGGER.debug("Started {}", this)
                oldHandle?.dispose()
                return newCoroutine
            }
        }
    }

    protected abstract suspend fun createCoroutineAsync(coroutineScope: CoroutineScope): Deferred<Result<T>>

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun emitUnchecked(result: Result<*>, source: Deferred<Result<*>>?) {
        emit(result as Result<T>, source as Deferred<Result<T>>)
    }

    @Suppress("DeferredResultUnused")
    suspend inline fun emit(result: Result<T>, source: Deferred<Result<T>>?) {
        if (result.isFailure) {
            AT_LOGGER.error("Emitting failure for {}", name, result.exceptionOrNull())
            timesFailed.incrementAndGet()
        } else {
            AT_LOGGER.debug("Emitting success for {}", name)
        }
        mutex.withLock(result) {
            if (cache.enabled && directDependentTasks.size < 2) {
                AT_LOGGER.info("Disabling caching for {} while emitting result", name)
                cache.enabled = false
            } else {
                AT_LOGGER.info("Emitting result of {} into cache", name)
                cache.set(result)
            }
            if (coroutine.compareAndSet(source, null)) {
                coroutineHandle.getAndSet(null)?.dispose()
            }
        }
        AT_LOGGER.debug("Unlocking {} after emitting result", name)
    }

    override suspend fun clearFailure() {
        AT_LOGGER.debug("Locking {} to clear failure", name)
        if (mutex.tryLock(this@AbstractTask)) {
            try {
                if (getNow()?.isFailure == true) {
                    AT_LOGGER.debug("Clearing failure from {}", name)
                    cache.set(null)
                    val oldCoroutine = coroutine.getAndSet(null)
                    if (oldCoroutine?.isCompleted == false) {
                        AT_LOGGER.debug("Canceling failed coroutine for {}", name)
                        oldCoroutine.cancel(CANCEL_BECAUSE_REPLACING)
                    }
                    coroutineHandle.getAndSet(null)?.dispose()
                    for (dependency in directDependencies) {
                        dependency.clearFailure()
                    }
                } else {
                    AT_LOGGER.debug("No failure to clear for {}", name)
                }
            } finally {
                AT_LOGGER.debug("Unlocking {} after clearing failure", name)
                mutex.unlock(this@AbstractTask)
            }
        } else {
            AT_LOGGER.warn("Couldn't acquire lock for {}.clearFailure()", name)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun mergeWithDuplicate(other: Task<*>): Task<T> {
        if (other === this || getNow() != null) {
            return this
        }
        val otherNow = other.getNow()
        if (otherNow != null) {
            emitUnchecked(otherNow, (other as? AbstractTask)?.coroutine?.get())
            return this
        }
        AT_LOGGER.debug("Locking {} to merge with a duplicate", name)
        mutex.withLock(other) {
            if (getNow() != null) {
                return@withLock
            }
            val result = other.getNow()
            if (result != null) {
                emitUnchecked(result, (other as? AbstractTask)?.coroutine?.get())
                return this
            }
            if (coroutine.get() == null && other is AbstractTask) {
                AT_LOGGER.debug("Locking {} to merge into {}", other.name, name)
                other.mutex.withLock(this@AbstractTask) {
                    val resultWithLock = other.getNow()
                    if (resultWithLock != null) {
                        emitUnchecked(resultWithLock, other.coroutine.get())
                        return this
                    }
                    val otherCoroutine = other.coroutine.get()
                    if (otherCoroutine != null) {
                        if (otherCoroutine.isCompleted) {
                            emitUnchecked(otherCoroutine.getCompleted(), otherCoroutine)
                        } else {
                            coroutine.set(createCoroutineScope().async { otherCoroutine.await() as Result<T> })
                        }
                        return this
                    }
                }
                AT_LOGGER.debug("Unlocking {} after merging it into {}", other.name, name)
            }
        }
        AT_LOGGER.debug("Unlocking {} after merging a duplicate into it", name)
        return this
    }

    private val coroutineName = CoroutineName(name)

    override suspend fun createCoroutineScope(): CoroutineScope = CoroutineScope(
        currentCoroutineContext()
            .plus(coroutineName)
            .plus(SUPERVISOR_JOB)
    )

    override fun isStartedOrAvailable(): Boolean = coroutine.get()?.isActive == true || getNow() != null

    override fun timesFailed(): Long = timesFailed.get()
}