package me.lyn1.netherRaids

import org.bukkit.plugin.java.JavaPlugin

class NetherRaids : JavaPlugin() {

    private lateinit var raidManager: RaidManager

    override fun onEnable() {
        // Plugin startup logic
        raidManager = RaidManager(this)
        val commandExecutor = NetherRaidCommand(raidManager)
        getCommand("netherraid")?.setExecutor(commandExecutor)
        getCommand("endnetherraid")?.setExecutor(commandExecutor)
        getCommand("getraidhorn")?.setExecutor(commandExecutor)
        server.pluginManager.registerEvents(RaidMobListener(raidManager), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
