package me.lyn1.netherRaids

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.ChatColor

class NetherRaidCommand(private val raidManager: RaidManager) : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be run by a player.")
            return true
        }

        when (command.name.toLowerCase()) {
            "netherraid" -> {
                if (args.size != 3) {
                    sender.sendMessage("${ChatColor.RED}Usage: /netherraid <difficulty> <number of waves> <radius>")
                    return false
                }

                val difficulty: Int
                val numberOfWaves: Int
                val radius: Int

                try {
                    difficulty = args[0].toInt()
                    numberOfWaves = args[1].toInt()
                    radius = args[2].toInt()
                } catch (e: NumberFormatException) {
                    sender.sendMessage("${ChatColor.RED}Difficulty, number of waves, and radius must be valid numbers.")
                    return false
                }

                if (difficulty < 1 || difficulty > 5) {
                    sender.sendMessage("${ChatColor.RED}Difficulty must be between 1 and 5.")
                    return false
                }
                if (numberOfWaves < 1) {
                    sender.sendMessage("${ChatColor.RED}Number of waves must be at least 1.")
                    return false
                }
                if (radius < 1) {
                    sender.sendMessage("${ChatColor.RED}Radius must be at least 1.")
                    return false
                }

                raidManager.startRaid(sender.location, difficulty, numberOfWaves, radius)
                return true
            }
            "endnetherraid" -> {
                if (args.isNotEmpty()) {
                    sender.sendMessage("${ChatColor.RED}Usage: /endnetherraid")
                    return false
                }
                raidManager.endRaid(sender.location)
                return true
            }
            else -> return false
        }
    }
}
