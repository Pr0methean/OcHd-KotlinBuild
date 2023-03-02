package io.github.pr0methean.ochd.texturebase

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.materials.block.pickaxe.Ore
import javafx.scene.paint.Color

suspend fun OutputTaskBuilder.redstoneOffAndOn(baseName: String,
                                               layers: LayerListBuilder.(redstoneStateColor: Color) -> Unit) {
    out(baseName) { layers(Color.BLACK) }
    out(baseName + "_on") { layers(Ore.REDSTONE.highlight) }
}
