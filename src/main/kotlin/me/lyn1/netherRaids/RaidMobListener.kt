package me.lyn1.netherRaids

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.SlimeSplitEvent
import org.bukkit.entity.MagmaCube

class RaidMobListener(private val raidManager: RaidManager) : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        raidManager.onMobDeath(entity.uniqueId)
    }

    @EventHandler
    fun onSlimeSplit(event: SlimeSplitEvent) {
        val slime = event.entity
        if (slime is MagmaCube) {
            // Check if this MagmaCube is part of an active raid
            val isRaidMob = raidManager.activeRaids.values.any { raidInstance ->
                raidInstance.spawnedMobs.contains(slime.uniqueId)
            }
            if (isRaidMob) {
                event.isCancelled = true // Prevent splitting
            }
        }
    }
}
