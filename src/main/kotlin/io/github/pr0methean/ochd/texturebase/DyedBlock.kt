package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.DYES
import io.github.pr0methean.ochd.tasks.ImageTask
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.util.concurrent.atomic.AtomicReference

abstract class DyedBlock(val name: String): Material {
    abstract suspend fun LayerListBuilder.createTextureLayers(color: Color)
    abstract suspend fun createSharedLayersTask(ctx: TaskPlanningContext): ImageTask
    var sharedLayersTaskRef = AtomicReference<ImageTask?>(null)

    override suspend fun outputTasks(ctx: TaskPlanningContext): Flow<OutputTask> = flow {
        sharedLayersTaskRef.compareAndSet(null, createSharedLayersTask(ctx))
        DYES.forEach {
            val dyeName = it.key
            val color = it.value
            emit(ctx.out(ctx.stack {createTextureLayers(color)}, "block/${dyeName}_$name"))
        }
    }
}