package me.lyn1.netherRaids

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDeathEvent
import org.bukkit.event.entity.SlimeSplitEvent
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason
import org.bukkit.entity.MagmaCube
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityTargetLivingEntityEvent
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.Location
import org.bukkit.util.BoundingBox

class RaidMobListener(private val raidManager: RaidManager) : Listener {

    @EventHandler
    fun onEntityDeath(event: EntityDeathEvent) {
        val entity = event.entity
        // Check if the entity is a raid mob
        val isRaidMob = raidManager.activeRaids.values.any { raidInstance ->
            raidInstance.spawnedMobs.contains(entity.uniqueId)
        }
        if (isRaidMob) {
            event.drops.clear() // Remove mob drops
        }
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
                event.isCancelled = true // Prevent splitting via SlimeSplitEvent
            }
        }
    }

    @EventHandler
    fun onCreatureSpawn(event: CreatureSpawnEvent) {
        if (event.entity is MagmaCube && event.spawnReason == SpawnReason.SLIME_SPLIT) {
            val spawnLocation = event.location
            val isWithinRaidRadius = raidManager.activeRaids.values.any { raidInstance ->
                raidInstance.center.world == spawnLocation.world &&
                raidInstance.center.distance(spawnLocation) <= raidInstance.radius * 1.1 // Check within raid radius + 10% buffer
            }
            if (isWithinRaidRadius) {
                event.isCancelled = true
            }
        }
    }

    @EventHandler
    fun onEntityTarget(event: EntityTargetLivingEntityEvent) {
        val entity = event.entity as? LivingEntity ?: return
        val target = event.target as? LivingEntity ?: return

        // Check if the entity is a raid mob
        val raidInstance = raidManager.activeRaids.values.firstOrNull { raid ->
            raid.spawnedMobs.contains(entity.uniqueId)
        } ?: return // Not a raid mob, do nothing

        // 1. Wither skeletons not mad at piglins or hoglins, and vice versa
        if (entity.type == EntityType.WITHER_SKELETON) {
            if (target.type == EntityType.PIGLIN || target.type == EntityType.HOGLIN || target.type == EntityType.PIGLIN_BRUTE) {
                event.isCancelled = true // Prevent Wither Skeletons from targeting Piglins, Hoglins, or Piglin Brutes
                return
            }
        } else if (entity.type == EntityType.PIGLIN || entity.type == EntityType.PIGLIN_BRUTE) {
            if (target.type == EntityType.WITHER_SKELETON) {
                event.isCancelled = true // Prevent Piglins/Piglin Brutes from targeting Wither Skeletons
                return
            }
        }

        // 2. Mobs AI want to stay within the radius and not leave it
        // If the target is outside the raid radius, cancel the target
        if (target.location.world != raidInstance.center.world ||
            raidInstance.center.distance(target.location) > raidInstance.radius * 1.2) { // Add a small buffer
            event.isCancelled = true
            return
        }

        // 3. Mobs get aggro to all players within the radius
        // If the mob is targeting a non-player, try to redirect to a player within radius
        if (target !is Player) {
            val boundingBox = BoundingBox.of(raidInstance.center, raidInstance.radius.toDouble() * 2, raidInstance.radius.toDouble() * 2, raidInstance.radius.toDouble() * 2)
            val playersInRadius = raidInstance.center.world?.getNearbyEntities(boundingBox)
                ?.filterIsInstance<Player>()
                ?.filter { it.location.distance(raidInstance.center) <= raidInstance.radius && it.gameMode == org.bukkit.GameMode.SURVIVAL }
                ?.shuffled() // Shuffle to pick a random player
            
            val closestPlayer = playersInRadius?.minByOrNull { it.location.distance(entity.location) }

            if (closestPlayer != null) {
                event.target = closestPlayer // Retarget to a player
                return
            } else {
                // If no players are nearby, and it's targeting a non-player, cancel the target
                event.isCancelled = true
                return
            }
        }
    }
}
