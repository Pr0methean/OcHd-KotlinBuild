package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingStats
import io.github.pr0methean.ochd.tasks.caching.DeferredTaskCache
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart.LAZY
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.util.StringBuilderFormattable
import java.io.PrintWriter
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext

val abstractTaskLogger: Logger = LogManager.getLogger("AbstractTask")

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
    private val directlyConsumingRepaintTasks = AtomicInteger(0)
    private val directlyConsumingNonRepaintTasks: AtomicInteger = AtomicInteger(0)

    fun isUsedOnlyForRepaints(): Boolean = directlyConsumingNonRepaintTasks.get() == 0

    private fun directConsumers(): Int = directlyConsumingRepaintTasks.get() + directlyConsumingNonRepaintTasks.get()

    private val dependenciesRegistered = AtomicBoolean(false)

    open fun appendForGraphPrinting(appendable: Appendable) {
        appendable.append(name)
    }

    private fun addDirectDependentTask(task: AbstractTask<*>) {
        if (task is RepaintTask) {
            directlyConsumingRepaintTasks.getAndIncrement()
        } else {
            directlyConsumingNonRepaintTasks.getAndIncrement()
        }
        if (directConsumers() >= 2 && cache.enable()) {
            ImageProcessingStats.onCachingEnabled(this)
        }
    }

    /**
     * Called once a dependent task has retrieved the output, so that we can disable caching and free up heap space once
     * all dependents have done so.
     */
    suspend fun removeDirectDependentTask(task: AbstractTask<*>) {
        val counter = if (task is RepaintTask) directlyConsumingRepaintTasks else directlyConsumingNonRepaintTasks
        check (counter.decrementAndGet() >= 0) {
            "Tried to remove more dependent tasks from $this than were added"
        }
        abstractTaskLogger.info("Removed dependency of {} on {}", task.name, name)
        if (directConsumers() == 0) {
            if (cache.disable()) {
                ImageProcessingStats.onCachingDisabled(this)
            }
            directDependencies.forEach { it.removeDirectDependentTask(this) }
        }
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
        abstractTaskLogger.debug("Computing hash code for {}", name)
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
     * If this task has only one direct-dependent task that's yet to start, we're worth 1 point to our direct and
     * transitive dependencies of that task because finishing that task will cause us to disable our cache.
     * If it has 2 direct-dependent tasks left to start, it's worth 1/4 point; if it has 3, it's worth 1/16 point,
     * and so on. Tasks with the highest cache-clearing coefficient are launched first when we're not immediately low
     * on memory.
     */
    suspend fun cacheClearingCoefficient(): Double {
        var coefficient = if (!cache.isEnabled()) {
            0.0
        } else {
            val unstartedDirectConsumers = directConsumers()
            val clearingScore = (1.0 / unstartedDirectConsumers).coerceAtLeast(java.lang.Double.MIN_NORMAL)
            if (isStartedOrAvailable()) {
                clearingScore
            } else -1.0 + clearingScore
        }
        for (task in directDependencies) {
            coefficient += task.cacheClearingCoefficient()
        }
        return coefficient
    }

    /**
     * True when this task should prepare to output an Image so that that Image can be cached, rather than rendering
     * onto a consuming task's canvas.
     */
    fun shouldRenderForCaching(): Boolean = isStartedOrAvailable()
            || (cache.isEnabled() && directConsumers() >= 2)
            || isStartedOrAvailable()

    suspend fun registerRecursiveDependencies() {
        if (dependenciesRegistered.compareAndSet(false, true)) {
            directDependencies.forEach {
                it.addDirectDependentTask(this@AbstractTask)
                it.registerRecursiveDependencies()
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
        abstractTaskLogger.debug("Creating a new coroutine for {}", name)
        coroutineScope.async(start = LAZY) { perform() }
    }.apply(Deferred<T>::start)

    abstract suspend fun perform(): T

    @Suppress("UNCHECKED_CAST", "DeferredResultUnused")
    open fun mergeWithDuplicate(other: AbstractTask<*>): AbstractTask<T> {
        abstractTaskLogger.debug("Merging {} with duplicate {}", name, other.name)
        if (other !== this && getNow() == null) {
            val otherCoroutine = other.cache.getNowAsync()
            if (otherCoroutine != null) {
                cache.computeIfAbsent { otherCoroutine as Deferred<T> }
            }
        }
        abstractTaskLogger.debug("Done merging {} with duplicate {}", name, other.name)
        return this
    }

    private fun isStartedOrAvailable(): Boolean = cache.getNowAsync()?.run { isActive || isCompleted } ?: false

    fun cacheableSubtasks(): Int {
        var subtasks = if (cache.isEnabled()) 1 else 0
        for (task in directDependencies) {
            subtasks += task.cacheableSubtasks()
        }
        return subtasks
    }

    /**
     * True if starting this task will not cause new entries to be added to the cache, other
     * than those that tasks already started are going to cache anyway. Tasks for which this
     * is true are prioritized when memory is low.
     */
    fun isCacheAllocationFreeOnMargin(): Boolean {
        if (isStartedOrAvailable()) {
            return true
        } else if (cache.isEnabled()) {
            return false
        }
        return directDependencies.all(AbstractTask<*>::isCacheAllocationFreeOnMargin)
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
