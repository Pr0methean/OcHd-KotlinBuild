package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask

interface Material {
    fun outputTasks(ctx: ImageProcessingContext): Sequence<OutputTask>
}