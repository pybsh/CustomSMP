package me.aroxu.customsmp.events

import me.aroxu.customsmp.CustomSMPPlugin
import org.bukkit.Bukkit
import org.bukkit.entity.Arrow
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.entity.Trident
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageByEntityEvent

class AttackEvent : Listener {
    @EventHandler
    fun onEntityDamageByEntity(event: EntityDamageByEntityEvent) {
        val target = event.entity
        val damager = event.damager
        if (target is Player && damager is Player) {
            if (CustomSMPPlugin.isInTeam[target.uniqueId]!! && CustomSMPPlugin.isInTeam[damager.uniqueId]!!) {
                if (CustomSMPPlugin.playerTeam[target.uniqueId]!! == CustomSMPPlugin.playerTeam[damager.uniqueId]!!) {
                    event.isCancelled = true
                }
            }
        } else if (target is Player && (damager is Arrow && damager.shooter is Player)) {
            if (CustomSMPPlugin.playerTeam[target.uniqueId]!! == CustomSMPPlugin.playerTeam[(damager.shooter as Player).uniqueId]!!) {
                event.isCancelled = true
            }
        } else if (target is Player && (damager is Trident && damager.shooter is Player)) {
            if (CustomSMPPlugin.playerTeam[target.uniqueId]!! == CustomSMPPlugin.playerTeam[(damager.shooter as Player).uniqueId]!!) {
                event.isCancelled = true
            }
        }

        if (CustomSMPPlugin.isInWar[target.uniqueId]!! &&
            (
                    (damager is Player && CustomSMPPlugin.isInWar[damager.uniqueId]!!) ||
                            (damager is Arrow && CustomSMPPlugin.isInWar[(damager.shooter as Player).uniqueId]!!) ||
                            (damager is Trident && CustomSMPPlugin.isInWar[(damager.shooter as Player).uniqueId]!!)
                    )
        ) {
            val task = Bukkit.getServer().scheduler.runTaskTimer(
                CustomSMPPlugin.plugin,
                Runnable {
                    if ((target as Player).isOnline) target.isGliding = false

                    if (damager is Arrow) if ((damager.shooter as Player).isOnline) (damager.shooter as Player).isGliding =
                        false
                    else if ((damager as Player).isOnline) (damager as Player).isGliding = false

                }, 0L, 0L
            )

            Bukkit.getServer().scheduler.runTaskLater(
                CustomSMPPlugin.plugin,
                Runnable {
                    task.cancel()
                }, 100L
            )
        }
        if(CustomSMPPlugin.invincibleTeams.any {CustomSMPPlugin.teamsMember[it]!!.any{ p -> p == target.uniqueId }}){
            event.isCancelled = true
        }
    }
}
