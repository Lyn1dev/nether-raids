package me.lyn1.netherRaids

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.entity.EntityType
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Zombie
import org.bukkit.entity.Piglin
import org.bukkit.entity.WitherSkeleton
import org.bukkit.inventory.ItemStack
import org.bukkit.Material
import org.bukkit.enchantments.Enchantment
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random
import org.bukkit.boss.BossBar
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class RaidManager(private val plugin: NetherRaids) {

    val activeRaids = ConcurrentHashMap<Location, RaidInstance>()

    data class RaidInstance(
        val center: Location,
        val initialDifficulty: Int,
        val numberOfWaves: Int,
        val radius: Int,
        var currentWave: Int,
        var currentDifficulty: Double,
        val spawnedMobs: MutableSet<UUID>,
        val bossBar: BossBar,
        var totalMobsInCurrentWave: Int
    )

    fun startRaid(playerLocation: Location, initialDifficulty: Int, numberOfWaves: Int, radius: Int) {
        val raidCenter = playerLocation.block.location // Use block location for raid center
        if (activeRaids.containsKey(raidCenter)) {
            playerLocation.world?.players?.forEach {
                if (it.location.distance(playerLocation) <= radius + 10) {
                    it.sendMessage("${ChatColor.RED}A Nether raid is already active at this location!")
                }
            }
            return
        }

        val bossBar = Bukkit.createBossBar(
            "${ChatColor.RED}Nether Raid - Wave 1",
            BarColor.RED,
            BarStyle.SOLID
        )
        bossBar.progress = 1.0 // Full at start

        playerLocation.world?.players?.forEach { player ->
            if (player.location.distance(playerLocation) <= radius + 10) {
                bossBar.addPlayer(player)
                player.sendMessage("${ChatColor.GOLD}Starting Nether raid with Difficulty: $initialDifficulty, Waves: $numberOfWaves, Radius: $radius")
            }
        }

        val raidInstance = RaidInstance(
            raidCenter,
            initialDifficulty,
            numberOfWaves,
            radius,
            1, // Start at wave 1
            initialDifficulty.toDouble(),
            ConcurrentHashMap.newKeySet(), // Thread-safe set for mob UUIDs
            bossBar,
            0 // Initialize totalMobsInCurrentWave
        )
        activeRaids[raidCenter] = raidInstance

        // Spawn the first wave immediately
        spawnWave(raidInstance)

        object : BukkitRunnable() {
            // mobsSpawnedThisWave is not used in this context, can remove or keep for future use

            override fun run() {
                val raidInstance = activeRaids[raidCenter] ?: run { cancel(); return } // Ensure raid is still active

                // Clean up phantom mobs before checking if empty
                raidInstance.spawnedMobs.removeIf { uuid ->
                    val entity = Bukkit.getEntity(uuid)
                    entity == null || !entity.isValid
                }

                // Check if all mobs from the current wave are defeated
                if (raidInstance.spawnedMobs.isEmpty()) {
                    if (raidInstance.currentWave >= raidInstance.numberOfWaves) { // Change to >= for final wave completion
                        raidInstance.bossBar.setTitle("${ChatColor.GREEN}Nether Raid Completed!")
                        raidInstance.bossBar.progress = 0.0
                        raidInstance.bossBar.removeAll()
                        activeRaids.remove(raidCenter)
                        cancel()
                        return
                    }

                    // Advance to next wave
                    raidInstance.currentWave++
                    raidInstance.currentDifficulty += 0.1 // Increase difficulty by 0.1 per wave

                    raidInstance.bossBar.setTitle("${ChatColor.RED}Nether Raid - Wave ${raidInstance.currentWave} / ${raidInstance.numberOfWaves} (Difficulty: ${String.format("%.1f", raidInstance.currentDifficulty)})")
                    raidInstance.bossBar.progress = 1.0 // Reset progress for new wave

                    playerLocation.world?.players?.forEach { player ->
                        if (player.location.distance(playerLocation) <= raidInstance.radius + 10 && !raidInstance.bossBar.players.contains(player)) {
                            raidInstance.bossBar.addPlayer(player)
                        }
                        if (player.location.distance(playerLocation) <= raidInstance.radius + 10) {
                            player.sendMessage("${ChatColor.GOLD}Starting Wave ${raidInstance.currentWave} / ${raidInstance.numberOfWaves} (Difficulty: ${String.format("%.1f", raidInstance.currentDifficulty)})")
                        }
                    }

                    spawnWave(raidInstance)
                } else {
                    // Mobs still alive, update boss bar and wait
                    updateBossBarProgress(raidInstance)
                }
            }
        }.runTaskTimer(plugin, 20L * 5, 20L * 5) // Start checking after 5 seconds, then every 5 seconds

    }

    private fun spawnWave(raidInstance: RaidInstance) {
        val world = raidInstance.center.world ?: return
        raidInstance.spawnedMobs.clear() // Clear mobs from previous wave

        val numberOfMobsPerGroup = (10 + raidInstance.currentDifficulty * 5).toInt()
        val numberOfGroups = when {
            raidInstance.currentWave == 1 -> 1
            raidInstance.currentDifficulty >= 2.0 -> Random.nextInt(2, 4) // 2 or 3 groups
            else -> 2 // Default to 2 groups for subsequent waves
        }

        var actualMobsSpawned = 0 // Track actual mobs spawned

        for (groupIndex in 0 until numberOfGroups) {
            val bannermenInGroup = Random.nextInt(1, 3) // 1 or 2 bannermen per group
            for (i in 0 until numberOfMobsPerGroup) {
                val spawnLocation = getRandomSpawnLocation(raidInstance.center, raidInstance.radius)
                if (spawnLocation == null) {
                    plugin.logger.warning("Could not find a safe spawn location for a mob in raid at ${raidInstance.center.blockX}, ${raidInstance.center.blockY}, ${raidInstance.center.blockZ}. Skipping mob spawn.")
                    continue
                }

                val entity: LivingEntity?
                val spawnedEntityType: EntityType? // To capture the type for logging
                if (i < bannermenInGroup) { // First 1 or 2 mobs are bannermen
                    entity = spawnBannerman(world, spawnLocation, raidInstance.currentDifficulty)
                    spawnedEntityType = EntityType.ZOMBIFIED_PIGLIN // Bannermen are Zombified Piglins
                } else {
                    val mobType = getRandomNetherMob(raidInstance.currentDifficulty)
                    entity = world.spawnEntity(spawnLocation, mobType) as? LivingEntity
                    spawnedEntityType = mobType
                }

                if (entity == null) {
                    plugin.logger.warning("Failed to spawn entity of type ${spawnedEntityType?.name ?: "UNKNOWN"} at ${spawnLocation.blockX}, ${spawnLocation.blockY}, ${spawnLocation.blockZ}. Skipping mob spawn.")
                    continue
                }

                applyDifficultyEffects(entity, raidInstance.currentDifficulty)
                raidInstance.spawnedMobs.add(entity.uniqueId)
                actualMobsSpawned++
            }
        }
        raidInstance.totalMobsInCurrentWave = actualMobsSpawned // Set the total mobs for the current wave
        updateBossBarProgress(raidInstance)
    }

    private fun spawnBannerman(world: World, location: Location, difficulty: Double): LivingEntity? {
        val bannermanType = EntityType.ZOMBIFIED_PIGLIN
        val bannerman = world.spawnEntity(location, bannermanType) as? LivingEntity ?: return null

        val equipment = bannerman.equipment
        if (equipment != null) {
            val banner = ItemStack(Material.RED_BANNER)
            equipment.setItemInOffHand(banner)
        }
        return bannerman
    }

    fun endRaid(playerLocation: Location) {
        val raidCenter = playerLocation.block.location
        val raidInstance = activeRaids[raidCenter]

        if (raidInstance != null) {
            raidInstance.bossBar.setTitle("${ChatColor.YELLOW}Nether Raid Ended by Command!")
            raidInstance.bossBar.progress = 0.0
            raidInstance.bossBar.removeAll()
            activeRaids.remove(raidCenter)
            playerLocation.world?.players?.forEach {
                if (it.location.distance(playerLocation) <= raidInstance.radius + 10) {
                    it.sendMessage("${ChatColor.YELLOW}The active Nether raid at your location has been ended.")
                }
            }
            // Despawn remaining mobs
            raidInstance.spawnedMobs.forEach { uuid ->
                val entity = Bukkit.getEntity(uuid)
                entity?.remove()
            }
            raidInstance.spawnedMobs.clear() // Clear the set after despawning
        } else {
            playerLocation.world?.players?.forEach {
                if (it.location.distance(playerLocation) <= 10) { // Check nearby players
                    it.sendMessage("${ChatColor.RED}No active Nether raid found at your location to end.")
                }
            }
        }
    }

    fun onMobDeath(mobUUID: UUID) {
        activeRaids.values.forEach { raidInstance ->
            if (raidInstance.spawnedMobs.remove(mobUUID)) {
                updateBossBarProgress(raidInstance)
                // No need for early wave completion logic here, the main runnable handles it.
                return@forEach // Mob found and removed, no need to check other raids
            }
        }
    }

    fun giveRaidHorn(player: Player) {
        val raidHorn = ItemStack(Material.GOAT_HORN)
        val meta = raidHorn.itemMeta
        meta?.setDisplayName("${ChatColor.GOLD}Nether Raid Horn")
        meta?.lore = listOf("${ChatColor.GRAY}Right-click to highlight active raid mobs!")
        raidHorn.itemMeta = meta
        player.inventory.addItem(raidHorn)
        player.sendMessage("${ChatColor.GOLD}You have received a Nether Raid Horn!")
    }

    fun highlightRaidMobs(player: Player) {
        var highlightedCount = 0
        activeRaids.values.forEach { raidInstance ->
            raidInstance.spawnedMobs.forEach { uuid ->
                val entity = Bukkit.getEntity(uuid) as? LivingEntity
                if (entity != null && entity.isValid && entity.location.world == player.world && entity.location.distance(player.location) <= raidInstance.radius + 20) {
                    // Apply glowing effect for a short duration (e.g., 10 seconds)
                    entity.addPotionEffect(PotionEffect(PotionEffectType.GLOWING, 20 * 10, 0, false, false))
                    highlightedCount++
                }
            }
        }
        if (highlightedCount > 0) {
            player.sendMessage("${ChatColor.GOLD}Highlighted $highlightedCount active raid mobs!")
        } else {
            player.sendMessage("${ChatColor.YELLOW}No active raid mobs found nearby to highlight.")
        }
    }

    private fun updateBossBarProgress(raidInstance: RaidInstance) {
        val totalMobsExpected = raidInstance.totalMobsInCurrentWave.toDouble() // Use the stored value
        val remainingMobs = raidInstance.spawnedMobs.size.toDouble()
        val progress = if (totalMobsExpected > 0) (remainingMobs / totalMobsExpected).coerceIn(0.0, 1.0) else 0.0
        raidInstance.bossBar.progress = progress

        if (progress < 0.20) {
            raidInstance.bossBar.setTitle("${ChatColor.RED}Nether Raid - Wave ${raidInstance.currentWave} - ${raidInstance.spawnedMobs.size} raiders left")
            // Constantly check for phantom mobs and clear them when below 20%
            raidInstance.spawnedMobs.removeIf { uuid ->
                val entity = Bukkit.getEntity(uuid)
                entity == null || !entity.isValid
            }
        } else {
            raidInstance.bossBar.setTitle("${ChatColor.RED}Nether Raid - Wave ${raidInstance.currentWave} / ${raidInstance.numberOfWaves} (Difficulty: ${String.format("%.1f", raidInstance.currentDifficulty)})")
        }
    }

    private fun getRandomSpawnLocation(center: Location, radius: Int): Location? {
        val random = Random
        val world = center.world ?: return null

        for (i in 0 until 50) { // Try 50 times to find a safe spawn location
            val xOffset = (random.nextDouble() * 2 - 1) * radius
            val zOffset = (random.nextDouble() * 2 - 1) * radius

            val testX = center.blockX + xOffset.toInt()
            val testZ = center.blockZ + zOffset.toInt()

            // Find the highest solid block at this X, Z
            var testY = world.maxHeight - 1
            while (testY > world.minHeight && !world.getBlockAt(testX, testY, testZ).type.isSolid) {
                testY--
            }

            // Check if the block found is solid and the two blocks above are air
            val blockBelow = world.getBlockAt(testX, testY, testZ)
            val blockAt = world.getBlockAt(testX, testY + 1, testZ)
            val blockAbove = world.getBlockAt(testX, testY + 2, testZ)

            if (blockBelow.type.isSolid && blockAt.type.isAir && blockAbove.type.isAir) {
                return Location(world, testX + 0.5, testY + 1.0, testZ + 0.5) // Spawn at the top of the solid block
            }
        }
        return null // Could not find a safe spawn location
    }

    private fun getRandomNetherMob(difficulty: Double): EntityType {
        val mobList = mutableListOf<EntityType>()

        // Base Nether mobs
        mobList.add(EntityType.ZOMBIFIED_PIGLIN)
        mobList.add(EntityType.MAGMA_CUBE)
        mobList.add(EntityType.BLAZE)

        // Stronger mobs based on difficulty
        if (difficulty >= 1.5) mobList.add(EntityType.WITHER_SKELETON)
        if (difficulty >= 2.5) mobList.add(EntityType.GHAST)
        if (difficulty >= 3.5) mobList.add(EntityType.HOGLIN)
        if (difficulty >= 4.5) mobList.add(EntityType.STRIDER) // Strider for variety, maybe ridden by piglins?

        return mobList.random()
    }

    private fun applyDifficultyEffects(entity: LivingEntity, difficulty: Double) {
        val random = Random

        // Health boost
        val maxHealth = entity.maxHealth + (difficulty * 2)
        entity.maxHealth = maxHealth
        entity.health = maxHealth // Heal to new max health

        // Potion effects
        if (difficulty >= 2.0 && random.nextDouble() < (difficulty / 5.0)) { // Chance to get effects
            entity.addPotionEffect(PotionEffect(PotionEffectType.STRENGTH, Int.MAX_VALUE, 0))
            entity.addPotionEffect(PotionEffect(PotionEffectType.RESISTANCE, Int.MAX_VALUE, 0))
        }

        // Equipment
        val equipment = entity.equipment ?: return

        // Armor
        if (random.nextDouble() < (difficulty / 5.0)) { // Chance to have armor
            val armorMaterial = when {
                difficulty >= 4.0 -> Material.DIAMOND_CHESTPLATE
                difficulty >= 3.0 -> Material.IRON_CHESTPLATE
                difficulty >= 2.0 -> Material.GOLDEN_CHESTPLATE
                else -> Material.LEATHER_CHESTPLATE
            }
            equipment.chestplate = ItemStack(armorMaterial)
            equipment.leggings = ItemStack(armorMaterial.name.replace("CHESTPLATE", "LEGGINGS").let { Material.valueOf(it) })
            equipment.boots = ItemStack(armorMaterial.name.replace("CHESTPLATE", "BOOTS").let { Material.valueOf(it) })
            equipment.helmet = ItemStack(armorMaterial.name.replace("CHESTPLATE", "HELMET").let { Material.valueOf(it) })

            // Enchant armor
            if (difficulty >= 3.0) {
                equipment.chestplate?.addEnchantment(Enchantment.PROTECTION, (difficulty / 2).toInt().coerceAtLeast(1))
                equipment.leggings?.addEnchantment(Enchantment.PROTECTION, (difficulty / 2).toInt().coerceAtLeast(1))
                equipment.boots?.addEnchantment(Enchantment.PROTECTION, (difficulty / 2).toInt().coerceAtLeast(1))
                equipment.helmet?.addEnchantment(Enchantment.PROTECTION, (difficulty / 2).toInt().coerceAtLeast(1))
            }
        }

        // Weapon
        if (random.nextDouble() < (difficulty / 4.0)) { // Chance to have a weapon
            val weaponMaterial = when {
                difficulty >= 4.5 -> Material.DIAMOND_SWORD
                difficulty >= 3.5 -> Material.IRON_SWORD
                difficulty >= 2.5 -> Material.GOLDEN_SWORD
                else -> Material.STONE_SWORD
            }
            equipment.setItemInMainHand(ItemStack(weaponMaterial))

            // Enchant weapon
            if (difficulty >= 3.0) {
                equipment.itemInMainHand?.addEnchantment(Enchantment.SHARPNESS, (difficulty / 2).toInt().coerceAtLeast(1))
            }
        }

        // Specific mob adjustments
        when (entity.type) {
            EntityType.WITHER_SKELETON -> {
                if (random.nextDouble() < (difficulty / 3.0)) { // Chance for bow
                    val bow = ItemStack(Material.BOW)
                    if (difficulty >= 3.0) {
                        bow.addEnchantment(Enchantment.POWER, (difficulty / 2).toInt().coerceAtLeast(1))
                        if (difficulty >= 4.0) bow.addEnchantment(Enchantment.PUNCH, 1)
                    }
                    equipment.setItemInMainHand(bow)
                }
            }
            EntityType.ZOMBIFIED_PIGLIN -> {
                if (entity is Zombie) { // Zombified Piglins are technically Zombies
                    entity.isBaby = false // Ensure not a baby zombie piglin
                }
                if (entity is Piglin) { // For future Piglin support if they become raid mobs
                    entity.isImmuneToZombification = true
                }
            }
            else -> {}
        }
    }
}
