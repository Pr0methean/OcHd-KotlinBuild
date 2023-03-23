package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.ImageProcessingStats.onCache
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.PrintWriter
import java.util.Collections.newSetFromMap
import java.util.WeakHashMap
import javax.annotation.concurrent.GuardedBy
import kotlin.coroutines.CoroutineContext

private val logger: Logger = LogManager.getLogger("AbstractTask")

/**
 * Unit of work that wraps its coroutine to support reuse (including under heap-constrained conditions).
 */
@Suppress("TooManyFunctions", "EqualsWithHashCodeExist", "EqualsOrHashCode")
// hashCode is cached in a lazy; equals isn't
abstract class AbstractTask<out T>(
    val name: String,
    val cache: DeferredTaskCache<@UnsafeVariance T>,
    val ctx: CoroutineContext
) : StringBuilderFormattable {
    val coroutineScope: CoroutineScope by lazy {
        CoroutineScope(ctx.plus(CoroutineName(name)))
    }
    @Volatile var dependenciesRegistered: Boolean = false

    open fun appendForGraphPrinting(appendable: Appendable) {
        appendable.append(name)
    }

    val mutex: Mutex = Mutex()

    @GuardedBy("mutex")
    val directDependentTasks: MutableSet<AbstractTask<*>> = newSetFromMap(WeakHashMap())
    private suspend fun addDirectDependentTask(task: AbstractTask<*>) {
        if (!directDependentTasks.contains(task) && mutex.withLock {
                directDependentTasks.add(task) && directDependentTasks.size >= 2 && cache.enable()
            }) {
            ImageProcessingStats.onCachingEnabled(this)
        }
    }

    /**
     * Called once a dependent task has retrieved the output, so that we can disable caching and free up heap space once
     * all dependents have done so.
     */
    suspend fun removeDirectDependentTask(task: AbstractTask<*>): Boolean {
        if (!directDependentTasks.contains(task)) {
            return false
        }
        mutex.withLock {
            if (!directDependentTasks.remove(task)) {
                return false
            }
            logger.info("Removed dependency of {} on {}", task.name, name)
            if (directDependentTasks.isEmpty() && cache.disable()) {
                ImageProcessingStats.onCachingDisabled(this)
                directDependencies.forEach { it.removeDirectDependentTask(this) }
            }
        }
        return true
    }

    val totalSubtasks: Int by lazy {
        var total = 1
        for (task in directDependencies) {
            total += task.totalSubtasks
        }
        total
    }
    abstract val directDependencies: Iterable<AbstractTask<*>>
    private val hashCode: Int by lazy {
        logger.debug("Computing hash code for {}", name)
        computeHashCode()
    }

    protected abstract fun computeHashCode(): Int

    fun startedOrAvailableSubtasks(): Int {
        if (isStartedOrAvailable()) {
            return totalSubtasks
        }
        var subtasks = 0
        for (task in directDependencies) {
            subtasks += task.startedOrAvailableSubtasks()
        }
        return subtasks
    }

    /**
     * True when this task should prepare to output an Image so that that Image can be cached, rather than rendering
     * onto a consuming task's canvas.
     */
    suspend fun shouldRenderForCaching(): Boolean = isStartedOrAvailable()
            || (cache.isEnabled() && mutex.withLock { directDependentTasks.size } > 1)
            || isStartedOrAvailable()

    suspend fun registerRecursiveDependencies() {
        if (!dependenciesRegistered) {
            mutex.withLock {
                if (!dependenciesRegistered) {
                    directDependencies.forEach {
                        it.addDirectDependentTask(this@AbstractTask)
                        it.registerRecursiveDependencies()
                    }
                    dependenciesRegistered = true
                }
            }
        }
    }

    final override fun toString(): String = name

    final override fun formatTo(buffer: StringBuilder) {
        buffer.append(name)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getNow(): T? {
        val coroutine = cache.getNowAsync() ?: return null
        return if (coroutine.isCompleted) {
            coroutine.getCompleted()
        } else null
    }

    @Suppress("DeferredIsResult")
    fun start(): Deferred<T> = cache.computeIfAbsent {
        logger.debug("Creating a new coroutine for {}", name)
        coroutineScope.async(start = LAZY) {
            perform().also {
                if (cache.isEnabled()) {
                    onCache(this@AbstractTask)
                }
            }
        }
    }.apply(Deferred<T>::start)

    abstract suspend fun perform(): T

    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    open fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<T> {
        logger.debug("Merging {} with duplicate {}", name, other.name)
        if (other !== this && getNow() == null) {
            val otherCoroutine = other.cache.getNowAsync()
            if (otherCoroutine != null) {
                cache.computeIfAbsent { otherCoroutine as Deferred<T> }
            }
        }
        logger.debug("Done merging {} with duplicate {}", name, other.name)
        return this
    }

    private fun isStartedOrAvailable(): Boolean = cache.getNowAsync()?.run { isActive || isCompleted } ?: false

    /**
     * How many new entries this task will cause to be added to the cache if we start it now.
     */
    fun newCacheEntries(): Int {
        if (isStartedOrAvailable()) {
            return 0
        }
        return directDependencies.sumOf(AbstractTask<*>::newCacheEntries) + if (cache.isEnabled()) 1 else 0
    }

    fun removedCacheEntries(): Int {
        return directDependencies.sumOf(AbstractTask<*>::removedCacheEntries) +
                if (cache.isEnabled() && directDependentTasks.size <= 1) 1 else 0
    }

    fun impendingCacheEntries(): Int {
        if (getNow() != null) {
            return 0
        }
        return directDependencies.sumOf(AbstractTask<*>::impendingCacheEntries) + if (cache.isEnabled()) 1 else 0
    }

    /**
     * Prints the dependency edges originating from this task in graphviz format.
     */
    fun printDependencies(writer: PrintWriter) {
        if (directDependencies.none()) return
        // "task" -> {"dep1" "dep2" }
        writer.print('\"')
        appendForGraphPrinting(writer)
        writer.print("\" -> {")
        directDependencies.forEach { dependency ->
            writer.print('\"')
            dependency.appendForGraphPrinting(writer)
            writer.print("\" ")
        }
        writer.println('}')
        directDependencies.forEach { it.printDependencies(writer) }
    }

    /**
     * True if this is weakly connected to [other] in a dependency graph (i.e. they share at
     * least one transitive dependency).
     */
    fun overlapsWith(other: AbstractTask<*>): Boolean = (this === other)
            || directDependencies.any(other::overlapsWith)
            || other.directDependencies.any(this::overlapsWith)

    suspend inline fun await(): T = start().await()
    override fun hashCode(): Int = hashCode
}
