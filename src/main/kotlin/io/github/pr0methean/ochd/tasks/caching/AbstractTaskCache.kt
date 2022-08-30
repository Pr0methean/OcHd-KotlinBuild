package io.github.pr0methean.ochd.tasks.caching

import org.apache.logging.log4j.LogManager

private val logger = LogManager.getLogger("AbstractTaskCache")
abstract class AbstractTaskCache<T>(val name: String) : TaskCache<T> {
    @Volatile var enabled = false
    override fun disable() {
        enabledSet(null)
        enabled = false
    }

    override fun enable() {
        enabled = true
    }

    override fun set(value: Result<T>?) {
        if (enabled) {
            logger.info("Caching result of {}", name)
            enabledSet(value)
        }
    }

    abstract fun enabledSet(value: Result<T>?)
}