package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDropItemEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemMergeEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Enforces the item-lifecycle flags (batch B8): item despawn/merge and the drops
 * of mobs, broken blocks and player deaths. All silent — these govern outcomes,
 * not player actions, so there is nothing to message.
 */
public final class ItemListener implements Listener {

    private final ZRegionsBukkitPlugin plugin;

    public ItemListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDespawn(ItemDespawnEvent event) {
        if (isDenied(Flags.ITEM_DESPAWN, event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemMerge(ItemMergeEvent event) {
        if (isDenied(Flags.ITEM_MERGE, event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    /** Non-player deaths: a player's loot goes through {@link #onPlayerDeath}. */
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event instanceof PlayerDeathEvent) {
            return;
        }
        if (isDenied(Flags.MOB_DROPS, event.getEntity().getLocation())) {
            event.getDrops().clear();
            event.setDroppedExp(0);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDropItem(BlockDropItemEvent event) {
        if (isDenied(Flags.BLOCK_DROPS, event.getBlock().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isDenied(Flags.DROP_ON_DEATH, event.getEntity().getLocation())) {
            event.getDrops().clear();
        }
    }

    private boolean isDenied(Flag<Boolean> flag, Location location) {
        boolean allowed = this.plugin.getRegionManager().resolveFlag(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), flag, null);
        return !allowed;
    }
}
