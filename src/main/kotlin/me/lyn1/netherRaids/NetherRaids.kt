package me.lyn1.netherRaids

import org.bukkit.plugin.java.JavaPlugin

class NetherRaids : JavaPlugin() {

    private lateinit var raidManager: RaidManager

    override fun onEnable() {
        // Plugin startup logic
        raidManager = RaidManager(this)
        getCommand("netherraid")?.setExecutor(NetherRaidCommand(raidManager))
        server.pluginManager.registerEvents(RaidMobListener(raidManager), this)
    }

    override fun onDisable() {
        // Plugin shutdown logic
    }
}
