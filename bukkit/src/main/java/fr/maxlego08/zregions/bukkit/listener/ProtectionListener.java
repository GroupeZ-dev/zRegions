package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Animals;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Translates native Bukkit events into flag resolutions against the region engine.
 * The listener only converts and cancels — the whole decision lives in common.
 */
public final class ProtectionListener implements Listener {

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
        if (event.getAction() == Action.PHYSICAL) {
            handleTrample(event);
            return;
        }
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

    // getBlock() is the block CraftBukkit actually changes: the fluid block on fill,
    // the receiving block on empty (which is the CLICKED one for waterloggables) —
    // clicked+face is one block off at region borders.
    @EventHandler(ignoreCancelled = true)
    public void onBucketFill(PlayerBucketFillEvent event) {
        check(Flags.BUCKET_FILL, event.getBlock(), event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        check(Flags.BUCKET_EMPTY, event.getBlock(), event.getPlayer(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand armorStand)) return;

        Player player = event.getPlayer();
        if (isDenied(player, Flags.ARMOR_STAND, armorStand.getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        if (isDenied(player, Flags.ITEM_DROP, player.getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (isDenied(player, Flags.ITEM_PICKUP, event.getItem().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    /**
     * A single handler covers both break causes: {@link HangingBreakByEntityEvent}
     * shares the parent's HandlerList, so a second handler would fire twice.
     */
    @EventHandler(ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        Location location = event.getEntity().getLocation();
        if (event instanceof HangingBreakByEntityEvent byEntity) {
            Player remover = resolveDamager(byEntity.getRemover());
            if (remover != null) {
                if (isDenied(remover, Flags.HANGING_BREAK, location)) {
                    event.setCancelled(true);
                    sendDeniedMessage(remover);
                }
                return;
            }
        }
        if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && isDeniedAt(Flags.ENTITY_EXPLOSION, location)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        Player player = event.getPlayer();
        Location location = event.getEntity().getLocation();
        if (player == null) {
            if (isDeniedAt(Flags.HANGING_PLACE, location)) {
                event.setCancelled(true);
            }
            return;
        }
        if (isDenied(player, Flags.HANGING_PLACE, location)) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof Vehicle)) return;

        Player player = event.getPlayer();
        Location location = event.getEntity().getLocation();
        if (player == null) {
            if (isDeniedAt(Flags.VEHICLE_PLACE, location)) {
                event.setCancelled(true);
            }
            return;
        }
        if (isDenied(player, Flags.VEHICLE_PLACE, location)) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lastDeniedMessage.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler(ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Player attacker = resolveDamager(event.getAttacker());
        if (attacker == null) return;

        if (isDenied(attacker, Flags.VEHICLE_DESTROY, event.getVehicle().getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(attacker);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player damager = resolveDamager(event.getDamager());
        if (damager == null) return;

        Entity victim = event.getEntity();
        Flag<Boolean> flag;
        if (victim instanceof Player) {
            flag = Flags.PVP;
        } else if (victim instanceof ArmorStand) {
            flag = Flags.ARMOR_STAND;
        } else if (victim instanceof Hanging) {
            // one punch on an item frame pops the displayed item — that IS a hanging break
            flag = Flags.HANGING_BREAK;
        } else if (victim instanceof Animals) {
            flag = Flags.DAMAGE_ANIMALS;
        } else {
            return;
        }

        if (isDenied(damager, flag, victim.getLocation())) {
            event.setCancelled(true);
            sendDeniedMessage(damager);
        }
    }

    private void handleTrample(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();
        if (type != Material.FARMLAND && type != Material.TURTLE_EGG) return;

        Player player = event.getPlayer();
        if (isDenied(player, Flags.CROP_TRAMPLE, block.getWorld().getName(), block.getX(), block.getY(), block.getZ())) {
            event.setUseInteractedBlock(Event.Result.DENY);
            sendDeniedMessage(player);
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

    private boolean isDenied(Player player, Flag<Boolean> flag, Location location) {
        return isDenied(player, flag, location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
    }

    private boolean isDenied(Player player, Flag<Boolean> flag, String worldName, double x, double y, double z) {
        if (player.hasPermission(this.plugin.getConfiguration().getBypassPermission())) return false;
        boolean allowed = this.plugin.getRegionManager().resolveFlag(worldName, x, y, z, flag, player.getUniqueId());
        return !allowed;
    }

    /** Environment-scoped resolution: no player, no bypass, silent at the call sites. */
    private boolean isDeniedAt(Flag<Boolean> flag, Location location) {
        boolean allowed = this.plugin.getRegionManager().resolveFlag(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), flag, null);
        return !allowed;
    }

    private void sendDeniedMessage(Player player) {
        long now = System.currentTimeMillis();
        Long last = this.lastDeniedMessage.get(player.getUniqueId());
        if (last != null && now - last < this.plugin.getConfiguration().getDenyMessageThrottleMillis()) return;
        this.lastDeniedMessage.put(player.getUniqueId(), now);
        this.plugin.getMessages().send(this.plugin.getPlayerFactory().wrap(player), Message.ACTION_DENIED);
    }
}
