package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskBuilder
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.texturebase.DoubleTallBlock
import javafx.scene.paint.Color

val stemColor: Color = c(0x4a8f28)
val stemShadow: Color = c(0x256325)
val stemHighlight: Color = c(0x55ab2d)

@Suppress("unused")
enum class DoubleTallFlower: DoubleTallBlock {
    SUNFLOWER {
        override fun LayerListBuilder.createBottomLayers() {
            layer("flowerStemTall", stemColor)
            layer("flowerStemTallBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override fun LayerListBuilder.createTopLayers() {
            layer("flowerStemShort", stemColor)
            layer("flowerStemShortBorder", stemHighlight)
            layer("flowerStemBottomBorder", stemShadow)
        }

        override fun OutputTaskBuilder.extraOutputTasks() {
            out("block/sunflower_back", "sunflowerPetals")
            out("block/sunflower_front") {
                layer("sunflowerPetals", Color.YELLOW)
                layer("sunflowerPistil")
            }
        }
    };
}
