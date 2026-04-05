package ge.macegate.yabosNewbieProt.listeners;

import ge.macegate.yabosNewbieProt.YabosNewbieProt;
import ge.macegate.yabosNewbieProt.model.RemovalReason;
import ge.macegate.yabosNewbieProt.managers.ProtectionManager;
import ge.macegate.yabosNewbieProt.util.MessageUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Locale;
import java.util.Map;

public class DamageListener implements Listener {

    private final YabosNewbieProt plugin;
    private final ProtectionManager manager;

    public DamageListener(YabosNewbieProt plugin, ProtectionManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Player victim = event.getEntity() instanceof Player player ? player : null;
        Player attacker = resolveAttacker(event);

        if (victim != null && manager.isProtected(victim.getUniqueId()) && manager.isProtectionActiveInWorld(victim.getWorld())) {
            if (attacker != null && plugin.getConfig().getBoolean("rules.block-incoming.player-damage", true)) {
                event.setCancelled(true);
                notifyAttacker(attacker, victim.getName(), "messages.combat.blocked-attacker",
                    "<red><white>{player}</white> is under newbie protection.</red>");
                return;
            }

            if (attacker == null && isMobDamage(event) && plugin.getConfig().getBoolean("rules.block-incoming.mob-damage", false)) {
                event.setCancelled(true);
                return;
            }
        }

        if (attacker != null && manager.isProtected(attacker.getUniqueId()) && manager.isProtectionActiveInWorld(attacker.getWorld())
            && victim != null && plugin.getConfig().getBoolean("rules.block-outgoing.player-damage", true)) {

            if (plugin.getConfig().getBoolean("rules.remove-protection-when-player-attacks", false)) {
                manager.removeProtection(attacker.getUniqueId(), RemovalReason.ATTACK_ACTION, true);
                return;
            }

            event.setCancelled(true);
            MessageUtil.send(attacker, plugin.getConfig(), "messages.combat.blocked-protected-attacker",
                "<yellow>You cannot damage players while your newbie protection is active.</yellow>",
                Map.of("player", victim.getName()));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!manager.isProtected(player.getUniqueId())) {
            return;
        }
        if (!manager.isProtectionActiveInWorld(player.getWorld())) {
            return;
        }

        if (!(event instanceof EntityDamageByEntityEvent)
            && plugin.getConfig().getBoolean("rules.block-incoming.environmental-damage", false)) {
            event.setCancelled(true);
        }
    }

    private void notifyAttacker(Player attacker, String protectedPlayerName, String path, String fallback) {
        if (!plugin.getConfig().getBoolean("notifications.attacker-message-enabled", true)) {
            return;
        }
        MessageUtil.send(attacker, plugin.getConfig(), path, fallback, Map.of("player", protectedPlayerName));
    }

    private boolean isMobDamage(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof LivingEntity && !(event.getDamager() instanceof Player)) {
            return true;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof LivingEntity living && !(living instanceof Player)) {
            return true;
        }
        return false;
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }
        if (event.getDamager() instanceof Projectile projectile && projectile.getShooter() instanceof Player player) {
            return player;
        }
        return null;
    }
}
