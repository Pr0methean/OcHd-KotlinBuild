package io.github.pr0methean.ochd.tasks

import io.github.pr0methean.ochd.ImageProcessingContext
import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import javafx.scene.paint.Color
import kotlinx.coroutines.flow.FlowCollector

suspend fun FlowCollector<OutputTask>.redstoneOffAndOn(ctx: ImageProcessingContext, baseName: String,
                                       layers: LayerListBuilder.(redstoneStateColor: Color) -> Unit) {
    emit(ctx.out(baseName) {layers(Color.BLACK)})
    emit(ctx.out(baseName + "_on") {layers(Ore.REDSTONE.highlight)})
}