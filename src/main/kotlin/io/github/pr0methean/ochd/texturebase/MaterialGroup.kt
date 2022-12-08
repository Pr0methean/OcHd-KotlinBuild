package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.FileOutputTask
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapConcat

@OptIn(FlowPreview::class)
open class MaterialGroup(private val elements: Flow<Material>): Material {
    constructor(vararg elements: Material): this(elements.asFlow())

    @OptIn(FlowPreview::class)
    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<FileOutputTask>
            = elements.flatMapConcat { it.outputTasks(ctx) }
}

inline fun <reified E> group(): MaterialGroup where E : Material, E : Enum<E> {
    return MaterialGroup(enumValues<E>().asFlow())
}
