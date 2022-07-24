package io.github.pr0methean.ochd

import kotlinx.coroutines.CoroutineScope
import java.util.concurrent.ConcurrentHashMap

class WritableImagePoolProvider(val standardPoolSize: Int, val nonsquarePoolSize: Int, val scopeForInit: CoroutineScope) {
    private val pools = ConcurrentHashMap<Pair<Int,Int>,WritableImagePool>()

    fun getPool(width: Int, height: Int) = pools.computeIfAbsent(width to height) {
        val capacity = if (width == height) {
            standardPoolSize
        } else {
            nonsquarePoolSize
        }
        WritableImagePool(width, height, capacity, scopeForInit)
    }
}