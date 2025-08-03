package me.lyn1.netherRaids

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent

class RaidMobListener(private val raidManager: RaidManager) : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        raidManager.onMobDeath(entity.uniqueId)
    }
}
