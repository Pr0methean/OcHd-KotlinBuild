package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.consumable.OutputConsumableTask
import kotlinx.coroutines.flow.Flow

interface Material {
    fun outputTasks(ctx: ImageProcessingContext): Flow<OutputConsumableTask>
}