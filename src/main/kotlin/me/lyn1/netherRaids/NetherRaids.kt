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

        // Schedule a repeating task to manage mob AI (aggro and radius)
        server.scheduler.runTaskTimer(this, Runnable {
            raidManager.activeRaids.values.forEach { raidInstance ->
                val raidCenter = raidInstance.center
                val raidRadius = raidInstance.radius.toDouble()

                raidInstance.spawnedMobs.forEach { uuid ->
                    val entity = Bukkit.getEntity(uuid) as? LivingEntity
                    if (entity != null && entity.isValid && entity.location.world == raidCenter.world) {
                        // Check if mob is outside raid radius
                        if (raidCenter.distance(entity.location) > raidRadius * 1.2) { // Use a slightly larger buffer for pulling back
                            // Teleport mob closer to the center or a player
                            val playersInRadius = raidCenter.world?.getNearbyEntities(raidCenter, raidRadius)
                                ?.filterIsInstance<Player>()
                                ?.filter { it.location.distance(raidCenter) <= raidRadius }
                                ?.shuffled()

                            val targetLocation = playersInRadius?.firstOrNull()?.location ?: raidCenter
                            entity.teleport(targetLocation)
                            // Optionally, clear target to force re-evaluation
                            if (entity is org.bukkit.entity.Mob) {
                                entity.target = null
                            }
                        }

                        // Ensure mob targets a player if no target or targeting non-player
                        if (entity is org.bukkit.entity.Mob && (entity.target == null || entity.target !is Player)) {
                            val playersInRadius = raidCenter.world?.getNearbyEntities(raidCenter, raidRadius)
                                ?.filterIsInstance<Player>()
                                ?.filter { it.location.distance(raidCenter) <= raidRadius }
                                ?.shuffled()

                            val closestPlayer = playersInRadius?.minByOrNull { it.location.distance(entity.location) }
                            if (closestPlayer != null) {
                                entity.target = closestPlayer
                            }
                        }
                    }
                }
            }
        }, 20L * 5, 20L * 5) // Run every 5 seconds
    }

    override fun onDisable() {
        // Plugin shutdown logic
        // Ensure all boss bars are removed on disable
        raidManager.activeRaids.values.forEach { raidInstance ->
            raidInstance.bossBar.removeAll()
            // Despawn remaining mobs
            raidInstance.spawnedMobs.forEach { uuid ->
                val entity = Bukkit.getEntity(uuid)
                entity?.remove()
            }
            raidManager.plugin.scoreboardManager.removeRaidTeam(raidInstance.center)
        }
        raidManager.activeRaids.clear()
    }
}
