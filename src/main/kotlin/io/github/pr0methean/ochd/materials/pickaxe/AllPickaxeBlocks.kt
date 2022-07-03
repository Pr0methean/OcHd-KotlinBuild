package io.github.pr0methean.ochd.materials.pickaxe

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.tasks.OutputTask

fun allPickaxeOutputTasks(ctx: ImageProcessingContext): List<OutputTask> {
    val output = mutableListOf<OutputTask>()
    output.addAll(OreBase.allOutputTasks(ctx))
    output.addAll(Ore.allOutputTasks(ctx))
    output.add(CutCopper.outputTask(ctx))
    return output
}