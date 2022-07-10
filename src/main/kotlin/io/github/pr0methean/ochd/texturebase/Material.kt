package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.flow.Flow

interface Material {
    fun rawOutputTasks(ctx: ImageProcessingContext): Flow<OutputTask>

    fun outputTasks(ctx: ImageProcessingContext) = ctx.decorateFlow(rawOutputTasks(ctx))
}