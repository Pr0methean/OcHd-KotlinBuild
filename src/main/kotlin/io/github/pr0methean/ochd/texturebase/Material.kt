package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask

interface Material {
    val name: String
    fun outputTasks(ctx: ImageProcessingContext): Iterable<OutputTask>
}