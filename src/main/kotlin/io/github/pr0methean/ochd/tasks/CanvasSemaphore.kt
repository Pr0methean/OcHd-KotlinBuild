package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.sync.Semaphore

val MAX_CANVASES: Int = Runtime.getRuntime().availableProcessors()

/**
 * Taken when creating a canvas, and released when it becomes unreachable. Keeps the number of canvases in the live set
 * under control.
 */
val CANVAS_SEMAPHORE: Semaphore = Semaphore(MAX_CANVASES)
