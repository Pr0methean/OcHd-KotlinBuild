package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.OutputTask
import kotlinx.coroutines.flow.Flow

interface Material {
    suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask>
}
