package me.lyn1.netherRaids

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

class RaidHornListener(private val raidManager: RaidManager) : Listener {

    private val cooldowns = ConcurrentHashMap<UUID, Long>()
    private val COOLDOWN_SECONDS = 20L

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val player = event.player
        val item = event.item ?: return

        // Check if it's a right-click action
        if (event.action != Action.RIGHT_CLICK_AIR && event.action != Action.RIGHT_CLICK_BLOCK) {
            return
        }

        // Check if the item is the custom Nether Raid Horn
        if (item.type == Material.GOAT_HORN && item.hasItemMeta()) {
            val meta = item.itemMeta
            if (meta?.hasDisplayName() == true && meta.displayName == "${ChatColor.GOLD}Nether Raid Horn") {
                // Check cooldown
                val lastUseTime = cooldowns[player.uniqueId] ?: 0L
                val currentTime = System.currentTimeMillis()
                val remainingCooldown = (lastUseTime + COOLDOWN_SECONDS * 1000 - currentTime) / 1000

                if (remainingCooldown > 0) {
                    player.sendMessage("${ChatColor.RED}You must wait ${remainingCooldown} seconds before using the horn again.")
                    return
                }

                // Use the horn
                cooldowns[player.uniqueId] = currentTime
                player.sendMessage("${ChatColor.GREEN}You used the Nether Raid Horn! Nearby raid mobs are highlighted.")

                // Highlight nearby raid mobs
                raidManager.highlightRaidMobs(player)
            }
        }
    }
}
