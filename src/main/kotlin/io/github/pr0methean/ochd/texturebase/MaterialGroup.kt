package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.tasks.PngOutputTask
import org.apache.logging.log4j.LogManager

private val LOGGER = LogManager.getLogger("MaterialGroup")
open class MaterialGroup(private val elements: Sequence<Material>): Material {
    constructor(vararg elements: Material): this(elements.asSequence())

    override fun outputTasks(ctx: TaskPlanningContext): Sequence<PngOutputTask>
            = elements.flatMap {
        LOGGER.debug("Emitting output tasks for material: {}", it)
        it.outputTasks(ctx)
    }
}

inline fun <reified E> group(): MaterialGroup where E : Material, E : Enum<E> {
    return MaterialGroup(enumValues<E>().asSequence())
}
