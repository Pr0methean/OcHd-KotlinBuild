package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import io.github.pr0methean.ochd.tasks.OutputTask
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.FlowCollector

suspend fun FlowCollector<OutputTask>.redstoneOffAndOn(ctx: ImageProcessingContext, baseName: String,
                                                       layers: suspend LayerListBuilder.(redstoneStateColor: Color) -> Unit) {
    emit(ctx.out({layers(Color.BLACK)}, baseName))
    emit(ctx.out({layers(Ore.REDSTONE.highlight)}, baseName + "_on"))
}