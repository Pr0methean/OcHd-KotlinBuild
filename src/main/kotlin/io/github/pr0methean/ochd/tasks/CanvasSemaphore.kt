package io.github.pr0methean.ochd.tasks

import kotlinx.coroutines.sync.Semaphore

const val MAX_CANVASES: Int = 8

/**
 * Taken when creating a canvas, and released when it becomes unreachable. Keeps the number of canvases in the live set
 * under control.
 */
val CANVAS_SEMAPHORE: Semaphore = Semaphore(MAX_CANVASES)
