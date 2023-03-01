package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.TaskPlanningContext
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.tasks.PngOutputTask
import javafx.scene.paint.Color

suspend fun SequenceScope<PngOutputTask>.redstoneOffAndOn(ctx: TaskPlanningContext, baseName: String,
                                                          layers:
                                                       LayerListBuilder.(redstoneStateColor: Color) -> Unit) {
    yield(ctx.out(baseName) { layers(Color.BLACK) })
    yield(ctx.out(baseName + "_on") { layers(Ore.REDSTONE.highlight) })
}
