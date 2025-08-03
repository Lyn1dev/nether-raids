package me.lyn1.netherRaids

import org.bukkit.plugin.java.JavaPlugin

class NetherRaids : JavaPlugin() {

    lateinit var raidManager: RaidManager // Make it accessible
    lateinit var scoreboardManager: ScoreboardManager

    override fun onEnable() {
        // Plugin startup logic
        scoreboardManager = ScoreboardManager(this)
        raidManager = RaidManager(this)
        val commandExecutor = NetherRaidCommand(raidManager)
        getCommand("netherraid")?.setExecutor(commandExecutor)
        getCommand("endnetherraid")?.setExecutor(commandExecutor)
        getCommand("getraidhorn")?.setExecutor(commandExecutor)
        server.pluginManager.registerEvents(RaidMobListener(raidManager), this)
        server.pluginManager.registerEvents(RaidHornListener(raidManager), this) // Register RaidHornListener
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
