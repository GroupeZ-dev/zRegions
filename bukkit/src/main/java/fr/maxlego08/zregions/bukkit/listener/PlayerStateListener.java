package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Enforces the player-condition flags (invincibility, fall damage, mob damage,
 * hunger, death keep-inventory/exp-drop). These flags protect the player, so
 * every denial is silent and the bypass permission does not apply.
 */
public final class PlayerStateListener implements Listener {

    private final ZRegionsBukkitPlugin plugin;

    public PlayerStateListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        // Invincible defaults to false — resolving to true means the region protects the player.
        if (resolve(Flags.INVINCIBLE, player)) {
            event.setCancelled(true);
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL && !resolve(Flags.FALL_DAMAGE, player)) {
            event.setCancelled(true);
            return;
        }
        // mob->player damage lives here, not in ProtectionListener: it protects the
        // victim (no bypass, silent) and EntityDamageByEntityEvent shares this
        // event's HandlerList anyway — a second handler would double-fire.
        if (event instanceof EntityDamageByEntityEvent byEntity
                && isMobDamager(byEntity.getDamager())
                && !resolve(Flags.MOB_DAMAGE, player)) {
            event.setCancelled(true);
        }
    }

    /** A non-player entity, projectiles-shot-by-players excluded (that is pvp's job). */
    private static boolean isMobDamager(Entity damager) {
        if (damager instanceof Player) return false;
        return !(damager instanceof Projectile projectile && projectile.getShooter() instanceof Player);
    }

    /**
     * Not cancellable — the death flags mutate the outcome instead: keep-inventory
     * (default deny, allow = the region preserves inventory and XP) and exp-drop
     * (deny = no XP orbs). Resolved where the player died.
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (resolve(Flags.KEEP_INVENTORY, player)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            return;
        }
        if (!resolve(Flags.EXP_DROP, player)) {
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (event.getFoodLevel() >= player.getFoodLevel()) return;

        if (!resolve(Flags.HUNGER, player)) {
            event.setCancelled(true);
        }
    }

    private boolean resolve(Flag<Boolean> flag, Player player) {
        Location location = player.getLocation();
        return this.plugin.getRegionManager().resolveFlag(player.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), flag, player.getUniqueId());
    }
}
