package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.tasks.caching.NoopTaskCache
import io.github.pr0methean.ochd.tasks.caching.TaskCache
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.Collections.newSetFromMap
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import javax.annotation.concurrent.GuardedBy
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

val AT_LOGGER: Logger = LogManager.getLogger("AbstractTask")
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
        var total = 1
        for (task in directDependencies) {
            total += task.totalSubtasks
        }
        total
    }

    override fun startedOrAvailableSubtasks(): Int {
        if (isStartedOrAvailable()) {
            return totalSubtasks
        }
        var subtasks = 0
        for (task in directDependencies) {
            subtasks += task.startedOrAvailableSubtasks()
        }
        return subtasks
    }

    override suspend fun registerRecursiveDependencies(): Unit = mutex.withLock {
        directDependencies.forEach {
            it.addDirectDependentTask(this@AbstractTask)
            it.registerRecursiveDependencies()
        }
    }

    final override fun toString(): String = name

    override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @GuardedBy("mutex")
    val coroutine: AtomicReference<Deferred<Result<T>>?> = AtomicReference(null)

    @OptIn(ExperimentalCoroutinesApi::class, InternalCoroutinesApi::class)
    override fun getNow(): Result<T>? {
        val cached = cache.getNow()
        if (cached != null) {
            AT_LOGGER.debug("Retrieved {} from cache", cached)
            return success(cached)
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
    override suspend fun startAsync(): Deferred<Result<T>> {
        val result = getNow()
        if (result != null) {
            return CompletableDeferred(result)
        }
        val maybeAlreadyStarted = coroutine.get()
        if (maybeAlreadyStarted != null) {
            return maybeAlreadyStarted
        }
        val newCoroutine = createCoroutineAsync()
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
                newCoroutine.start()
                AT_LOGGER.debug("Started {}", this)
                return newCoroutine
            }
        }
    }

    protected abstract suspend fun createCoroutineAsync(): Deferred<Result<T>>

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun emitUnchecked(result: Result<*>) {
        emit(result as Result<T>)
    }

    @Suppress("DeferredResultUnused")
    suspend inline fun emit(result: Result<T>) {
        if (result.isFailure) {
            AT_LOGGER.error("Emitting failure for {} due to {}", name, result.exceptionOrNull()?.message)
            timesFailed.getAndIncrement()
        } else {
            AT_LOGGER.debug("Emitting success for {}", name)
        }
        mutex.withLock(result) {
            if (result.isSuccess) {
                if (cache.enabled && directDependentTasks.size < 2) {
                    AT_LOGGER.info("Disabling caching for {} while emitting result", name)
                    cache.enabled = false
                } else {
                    cache.set(result.getOrThrow())
                }
            }
            coroutine.set(null)
        }
        AT_LOGGER.debug("Unlocking {} after emitting result", name)
    }

    @Suppress("UNCHECKED_CAST")
    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun mergeWithDuplicate(other: Task<*>): Task<T> {
        if (other === this || getNow() != null) {
            return this
        }
        val otherNow = other.getNow()
        if (otherNow != null) {
            emitUnchecked(otherNow)
            return this
        }
        AT_LOGGER.debug("Locking {} to merge with a duplicate", name)
        mutex.withLock(other) {
            if (getNow() != null) {
                return@withLock
            }
            val result = other.getNow()
            if (result != null) {
                emitUnchecked(result)
                return this
            }
            if (coroutine.get() == null && other is AbstractTask) {
                AT_LOGGER.debug("Locking {} to merge into {}", other.name, name)
                other.mutex.withLock(this@AbstractTask) {
                    val resultWithLock = other.getNow()
                    if (resultWithLock != null) {
                        emitUnchecked(resultWithLock)
                        return this
                    }
                    val otherCoroutine = other.coroutine.get()
                    if (otherCoroutine != null) {
                        if (otherCoroutine.isCompleted) {
                            emitUnchecked(otherCoroutine.getCompleted())
                        } else {
                            coroutine.set(getCoroutineScope().async { otherCoroutine.await() as Result<T> })
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

    @Volatile private var coroutineScope: CoroutineScope? = null

    override suspend fun getCoroutineScope(): CoroutineScope {
        var scopeNow = coroutineScope
        if (scopeNow == null) {
            scopeNow = mutex.withLock {
                var scopeWithLock = coroutineScope
                if (scopeWithLock == null) {
                    scopeWithLock = CoroutineScope(
                        currentCoroutineContext()
                            .plus(CoroutineName(name))
                            .plus(SUPERVISOR_JOB)
                    )
                    coroutineScope = scopeWithLock
                }
                scopeWithLock
            }
        }
        return scopeNow
    }

    override fun isStartedOrAvailable(): Boolean = coroutine.get()?.isActive == true || getNow() != null

    override fun timesFailed(): Long = timesFailed.get()
    override fun cacheableSubtasks(): Int {
        var subtasks = if (cache.enabled) 1 else 0
        for (task in directDependencies) {
            subtasks += task.cacheableSubtasks()
        }
        return subtasks
    }
}
