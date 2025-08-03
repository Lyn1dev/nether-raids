package me.lyn1.netherRaids

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.entity.LivingEntity
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.concurrent.ConcurrentHashMap

class ScoreboardManager(private val plugin: NetherRaids) {

    private val raidScoreboards = ConcurrentHashMap<Location, Scoreboard>()
    private val raidTeams = ConcurrentHashMap<Location, Team>()

    fun createRaidTeam(raidCenter: Location): Team {
        val scoreboard = Bukkit.getScoreboardManager()?.newScoreboard ?: Bukkit.getScoreboardManager()!!.mainScoreboard
        raidScoreboards[raidCenter] = scoreboard

        val teamName = "nether_raid_${raidCenter.blockX}_${raidCenter.blockY}_${raidCenter.blockZ}"
        var team = scoreboard.getTeam(teamName)
        if (team == null) {
            team = scoreboard.registerNewTeam(teamName)
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
            team.setOption(Team.Option.DEATH_MESSAGE_VISIBILITY, Team.OptionStatus.NEVER)
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS)
            team.color = ChatColor.RED
        }
        raidTeams[raidCenter] = team
        return team
    }

    fun addMobToRaidTeam(entity: LivingEntity, raidCenter: Location) {
        val team = raidTeams[raidCenter] ?: createRaidTeam(raidCenter)
        if (!team.hasEntry(entity.uniqueId.toString())) {
            team.addEntry(entity.uniqueId.toString())
        }
    }

    fun removeMobFromRaidTeam(entity: LivingEntity, raidCenter: Location) {
        val team = raidTeams[raidCenter]
        if (team != null && team.hasEntry(entity.uniqueId.toString())) {
            team.removeEntry(entity.uniqueId.toString())
        }
    }

    fun removeRaidTeam(raidCenter: Location) {
        val team = raidTeams.remove(raidCenter)
        team?.unregister()
        raidScoreboards.remove(raidCenter)
    }
}
