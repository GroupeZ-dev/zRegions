package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;

/**
 * Enforces the player-condition flags (invincibility, fall damage, hunger).
 * These flags protect the player, so every denial is silent and the bypass
 * permission does not apply.
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
