package io.github.pr0methean.ochd.tasks.consumable.caching

/**
 * If a task uses this, results will not be stored. Any requests for the result of the task will
 * trigger a new computation if there isn't already one in progress.
 */
class NoopTaskCache<T>: TaskCache<T> {
    override fun getNow(): Result<T>? = null

    override fun set(value: Result<T>?) {
        // No-op.
    }
}