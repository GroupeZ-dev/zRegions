package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enforces the environment flags: events with no player behind them (redstone,
 * pistons, fire, fluids, explosions, mobs). Every denial is silent — there is
 * nobody to message — except fire ignition by a player, throttled like
 * {@link ProtectionListener}.
 */
public final class EnvironmentProtectionListener implements Listener {

    private static final String BYPASS_PERMISSION = "zregions.bypass";
    private static final long MESSAGE_THROTTLE_MILLIS = 2000L;

    /**
     * The natural-spawn reasons the {@code mob-spawning} flag blocks. A denylist,
     * not an allowlist: cancelling conversion spawns (DROWNED, FROZEN, INFECTION…)
     * silently DELETES the original mob (the server discards it regardless of the
     * event outcome), and player/plugin-driven reasons must never be blocked.
     */
    private static final Set<CreatureSpawnEvent.SpawnReason> BLOCKED_SPAWN_REASONS = EnumSet.of(
            CreatureSpawnEvent.SpawnReason.NATURAL,
            CreatureSpawnEvent.SpawnReason.SPAWNER,
            CreatureSpawnEvent.SpawnReason.PATROL,
            CreatureSpawnEvent.SpawnReason.RAID,
            CreatureSpawnEvent.SpawnReason.REINFORCEMENTS,
            CreatureSpawnEvent.SpawnReason.SILVERFISH_BLOCK,
            CreatureSpawnEvent.SpawnReason.VILLAGE_INVASION,
            CreatureSpawnEvent.SpawnReason.VILLAGE_DEFENSE,
            CreatureSpawnEvent.SpawnReason.NETHER_PORTAL,
            CreatureSpawnEvent.SpawnReason.LIGHTNING,
            CreatureSpawnEvent.SpawnReason.TRAP,
            CreatureSpawnEvent.SpawnReason.JOCKEY,
            CreatureSpawnEvent.SpawnReason.MOUNT,
            CreatureSpawnEvent.SpawnReason.DEFAULT);

    private final ZRegionsBukkitPlugin plugin;
    private final Map<UUID, Long> lastDeniedMessage = new ConcurrentHashMap<>();

    public EnvironmentProtectionListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /** Not cancellable — restoring the old current is the API's way of denying. */
    @EventHandler
    public void onRedstone(BlockRedstoneEvent event) {
        if (event.getNewCurrent() == event.getOldCurrent()) return;
        if (isDenied(Flags.REDSTONE, event.getBlock())) {
            event.setNewCurrent(event.getOldCurrent());
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (isPistonDenied(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (isPistonDenied(event.getBlock(), event.getBlocks(), event.getDirection())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        if (player == null) {
            if (isDenied(Flags.FIRE_SPREAD, event.getBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        if (isDenied(player, Flags.FIRE_IGNITE, event.getBlock())) {
            event.setCancelled(true);
            sendDeniedMessage(player);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() != Material.FIRE) return;
        if (isDenied(Flags.FIRE_SPREAD, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        if (isDenied(Flags.FIRE_SPREAD, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        if (isDenied(Flags.FLUID_FLOW, event.getToBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLeavesDecay(LeavesDecayEvent event) {
        if (isDenied(Flags.LEAF_DECAY, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        filterExplodedBlocks(event.blockList(), Flags.ENTITY_EXPLOSION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        filterExplodedBlocks(event.blockList(), Flags.BLOCK_EXPLOSION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!BLOCKED_SPAWN_REASONS.contains(event.getSpawnReason())) return;

        Location location = event.getLocation();
        boolean allowed = this.plugin.getRegionManager().resolveFlag(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), Flags.MOB_SPAWNING, null);
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntity() instanceof Player) return;
        if (isDenied(Flags.MOB_GRIEFING, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Mob trampling of farmland/turtle eggs (a player trample goes through {@link ProtectionListener}). */
    @EventHandler(ignoreCancelled = true)
    public void onEntityInteract(EntityInteractEvent event) {
        Material type = event.getBlock().getType();
        if (type != Material.FARMLAND && type != Material.TURTLE_EGG) return;
        if (isDenied(Flags.MOB_GRIEFING, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        this.lastDeniedMessage.remove(event.getPlayer().getUniqueId());
    }

    /** The piston itself, every moved block and every destination must all allow the move. */
    private boolean isPistonDenied(Block piston, List<Block> moved, BlockFace direction) {
        if (isDenied(Flags.PISTON, piston)) return true;
        for (Block block : moved) {
            if (isDenied(Flags.PISTON, block) || isDenied(Flags.PISTON, block.getRelative(direction))) {
                return true;
            }
        }
        return false;
    }

    private void filterExplodedBlocks(List<Block> blocks, Flag<Boolean> flag) {
        blocks.removeIf(block -> isDenied(flag, block));
    }

    private boolean isDenied(Flag<Boolean> flag, Block block) {
        boolean allowed = this.plugin.getRegionManager().resolveFlag(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), flag, null);
        return !allowed;
    }

    private boolean isDenied(Player player, Flag<Boolean> flag, Block block) {
        if (player.hasPermission(BYPASS_PERMISSION)) return false;
        boolean allowed = this.plugin.getRegionManager().resolveFlag(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), flag, player.getUniqueId());
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
