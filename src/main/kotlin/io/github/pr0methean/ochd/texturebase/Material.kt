package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask

interface Material {
    fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask>
}
