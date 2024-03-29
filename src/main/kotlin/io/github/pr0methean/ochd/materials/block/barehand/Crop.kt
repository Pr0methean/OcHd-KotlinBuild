package io.github.pr0methean.ochd.materials.block.barehand

import io.github.pr0methean.ochd.LayerListBuilder
import io.github.pr0methean.ochd.OutputTaskEmitter
import io.github.pr0methean.ochd.c
import io.github.pr0methean.ochd.materials.block.axe.Fungus
import io.github.pr0methean.ochd.texturebase.Material
import javafx.scene.paint.Paint
import java.util.*

/** Crops with multiple growth stages. */
@Suppress("unused")
private val vegLeavesColor = c(0x4a8f28)
private val vegLeavesShadow = c(0x256325)
private val vegLeavesHighlight = c(0x55ff2d)
private val wheatColor = c(0x888836)
private val wheatShadow = c(0x636300)
private val wheatHighlight = c(0xdcbb65)

@Suppress("unused")
enum class Crop(private val numStages: Int, val color: Paint): Material {
    NETHER_WART(3, Fungus.CRIMSON.leavesShadow) {
        override fun LayerListBuilder.createTextureForStage(stage: Int) {
            layer("wart$stage", color)
            if (stage == 2) {
                layer("wart2a", Fungus.CRIMSON.leavesHighlight)
            }
        }
    },
    CARROTS(4, c(0xff8a00)),
    BEETROOTS(4, c(0xbf2727)),
    POTATOES(4, c(0xd97b30)) {
        override fun LayerListBuilder.createTextureForFinalStage() {
            layer("flowerStemShort", vegLeavesHighlight)
            layer("potato", color)
        }
    },
    WHEAT(8, c(0x888836)) {
        override fun LayerListBuilder.createTextureForStage(stage: Int) {
            val firstColor = if (stage == 7) wheatHighlight else wheatShadow
            layer("wheat$stage", firstColor)
            layer("wheatTexture$stage", wheatColor)
        }
    };

    override fun OutputTaskEmitter.outputTasks() {
        for (stage in 0 until numStages) {
            out("block/${this@Crop.name}_stage${stage}") { createTextureForStage(stage) }
        }
    }

    open fun svgBaseName(): String = name.lowercase(Locale.ENGLISH)

    open fun LayerListBuilder.createTextureForStage(stage: Int) {
        if (stage == numStages - 1) {
            createTextureForFinalStage()
        } else {
            layer("${svgBaseName()}${stage}", vegLeavesShadow)
        }
    }

    open fun LayerListBuilder.createTextureForFinalStage() {
        layer("${svgBaseName()}${numStages - 1}Stems", vegLeavesHighlight)
        layer("rootVeg", color)
    }
}
