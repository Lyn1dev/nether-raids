# Nether Raids Plugin

This Spigot/Paper plugin introduces dynamic Nether Raids to your Minecraft server, providing challenging PVE encounters for players. Raids feature escalating difficulty, multiple waves of custom mobs, and unique mechanics like bannermen and a raid horn.

## Features

*   **Dynamic Raid Spawning**: Start a raid at any location with configurable difficulty, number of waves, and radius.
*   **Escalating Difficulty**: Each wave increases in difficulty, making mobs stronger and more resilient.
*   **Custom Mobs**: Encounter a variety of Nether mobs, including Piglins, Hoglins, Wither Skeletons, and more, with enhanced attributes and equipment based on raid difficulty.
*   **Bannermen**: Special Zombified Piglin mobs that carry unique banners, adding a visual and strategic element to the raid.
*   **Raid Boss Bar**: A custom boss bar displays raid progress, current wave, and remaining mobs.
*   **Raid Horn**: Players can receive a special "Nether Raid Horn" item to highlight active raid mobs within a certain radius.
*   **Mob AI Adjustments**: Raid mobs are designed to stay within the raid radius and prioritize targeting players.
*   **Slime Split Prevention**: Magma Cubes part of a raid will not split into smaller slimes upon death.

## Commands

The primary command to manage raids is `/netherraid`.

*   `/netherraid start [difficulty] [waves] [radius]`
    *   Starts a new Nether Raid at your current location.
    *   `[difficulty]`: An integer representing the initial difficulty (e.g., 1-5). This affects mob health, potion effects, and equipment.
    *   `[waves]`: An integer representing the total number of waves in the raid.
    *   `[radius]`: An integer representing the radius around the raid center where mobs will spawn and players will be added to the boss bar.
    *   **Example**: `/netherraid start 2 5 30` (Starts a raid with difficulty 2, 5 waves, and a 30-block radius)

*   `/netherraid end`
    *   Ends the closest active Nether Raid to your current location. All remaining raid mobs will be despawned.

*   `/netherraid horn`
    *   Gives you a "Nether Raid Horn" item. Right-click with this item to highlight active raid mobs within a certain range.

## Raid Mechanics

### Starting a Raid
A raid is initiated using the `/netherraid start` command. If a raid is already active at the specified location, a message will inform the player.

### Waves and Difficulty
Raids progress through a set number of waves. With each new wave, the `currentDifficulty` increases by 0.5, up to a maximum of 5.0. This increased difficulty translates to:
*   More mobs per wave.
*   Increased health for spawned mobs.
*   Higher chance for mobs to receive Strength and Resistance potion effects.
*   Better armor and weapons for mobs, with higher enchantment levels.

### Mob Spawning
Mobs spawn within the defined raid radius. A central spawn point is determined for each group of mobs to ensure they are somewhat clustered. Bannermen (Zombified Piglins with custom banners) are included in each wave.

### Bannermen
Bannermen are special Zombified Piglins equipped with a red banner featuring black triangles and a red border. They are spawned as part of the regular mob waves.

### Raid Boss Bar
A boss bar at the top of the screen provides real-time information about the raid:
*   Current wave number and total waves.
*   Current difficulty.
*   Number of remaining raiders in the current wave.
*   Progress bar indicating the percentage of mobs defeated in the current wave.
Players entering or leaving the raid radius will be automatically added to or removed from the boss bar.

### Mob Behavior
*   **Targeting**: Raid mobs will primarily target players within the raid radius. If a mob targets a non-player, it will attempt to redirect its target to the closest player.
*   **Radius Adherence**: Mobs are programmed to stay within the raid radius. If a mob's target moves outside this radius, the mob will stop targeting it.
*   **Magma Cube Splitting**: Magma Cubes that are part of a raid will not split when killed, preventing an overwhelming number of smaller slimes.

### Ending a Raid
A raid automatically ends when all waves are completed and all mobs are defeated. Players can also manually end the closest active raid using the `/netherraid end` command, which will despawn all remaining raid mobs.

## Installation

1.  Download the plugin JAR file.
2.  Place the JAR file into your server's `plugins` folder.
3.  Restart or reload your server.

## Configuration

(No specific configuration options are currently exposed in `plugin.yml` beyond basic plugin metadata.)

## Development

This plugin is developed using Kotlin and the Spigot API.
