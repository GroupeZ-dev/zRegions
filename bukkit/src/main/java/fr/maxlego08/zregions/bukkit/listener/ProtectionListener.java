package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates native Bukkit events into flag resolutions against the region engine.
 * The listener only converts and cancels — the whole decision lives in common.
 */
public final class ProtectionListener implements Listener {

    private static final String BYPASS_PERMISSION = "zregions.bypass";
    private static final long MESSAGE_THROTTLE_MILLIS = 2000L;

    private final ZRegionsBukkitPlugin plugin;
    private final Map<UUID, Long> lastDeniedMessage = new ConcurrentHashMap<>();

    public ProtectionListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        check(Flags.BLOCK_BREAK, event.getBlock(), event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        check(Flags.BLOCK_PLACE, event.getBlock(), event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        Block block = event.getClickedBlock();
        if (block == null) return;

        Flag<Boolean> flag = block.getState() instanceof Container ? Flags.CONTAINER_ACCESS : Flags.INTERACT;
        Player player = event.getPlayer();
        if (isDenied(player, flag, block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) return;

        Location location = victim.getLocation();
        if (isDenied(damager, Flags.PVP, victim.getWorld().getName(), location.getX(), location.getY(), location.getZ())) {
            event.setCancelled(true);
            sendDeniedMessage(damager);
        }
    }

    private Player resolveDamager(Entity damager) {
        if (damager instanceof Player player) return player;
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter) {
            return shooter;
        }
        return null;
    }

    private void check(Flag<Boolean> flag, Block block, Player player, Cancellable event) {
        if (isDenied(player, flag, block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    private boolean isDenied(Player player, Flag<Boolean> flag, String worldName, double x, double y, double z) {
        if (player.hasPermission(BYPASS_PERMISSION)) return false;
        boolean allowed = this.plugin.getRegionManager().resolveFlag(worldName, x, y, z, flag, player.getUniqueId());
        return !allowed;
    }

    private void sendDeniedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = this.lastDeniedMessage.get(player.getUniqueId());
        if (last != null && now - last < MESSAGE_THROTTLE_MILLIS) return;
        this.lastDeniedMessage.put(player.getUniqueId(), now);
        this.plugin.getMessages().send(this.plugin.getPlayerFactory().wrap(player), Message.ACTION_DENIED);
    }
}
