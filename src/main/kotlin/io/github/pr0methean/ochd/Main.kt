package io.github.pr0methean.ochd

import io.github.pr0methean.ochd.enums.Dye
import kotlinx.coroutines.GlobalScope
import java.io.File

fun main(args:Array<String>) {
    if (args.isEmpty()) {
        println("Usage: main <size>")
        return
    }

    val tileSize = args[0].toInt()
    if (tileSize <= 0) throw IllegalArgumentException("tileSize shouldn't be zero or negative but was ${args[0]}")

    // List constants
    val OVERWORLD_WOODS = listOf("acacia", "birch", "dark_oak", "jungle", "mangrove", "oak", "spruce")
    val FUNGI = listOf("crimson", "warped")
    val WOODS = listOf(OVERWORLD_WOODS, FUNGI).flatten()
    val SIMPLE_ORES = listOf("coal", "copper", "iron", "redstone", "gold", "quartz")
    val ORES = listOf(SIMPLE_ORES, listOf("lapis", "diamond", "emerald")).flatten()
    val COMMAND_BLOCK_TYPES = listOf("command_block", "repeating_command_block", "chain_command_block")
    val NORMAL_MUSIC_DISCS = listOf("far", "wait", "strad", "mall", "cat", "pigstep", "mellohi", "13", "blocks", "stal",
            "ward", "5", "otherside", "chirp")
    val DISC_LABEL_COLORS = listOf(Dye.values()).subList(1, Dye.values().size - 1)
    val OXIDATION_STATES = listOf("exposed", "weathered", "oxidized")

    val ctx = ImageProcessingContext(tileSize, GlobalScope, File("svg"), File("out"))
    ctx.run {


    }
}