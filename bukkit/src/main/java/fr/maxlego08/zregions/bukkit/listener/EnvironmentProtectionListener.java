package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.locale.Message;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Snowman;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.LingeringPotionSplashEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.LightningStrikeEvent;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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

    /**
     * Reasons safe to cancel WITHOUT deleting a source mob: the blocked-set (natural,
     * spawner, raid…) plus the purely-additive egg/command reasons that the fine spawn
     * flags may also govern (mob-spawning still only covers the blocked set).
     */
    private static final Set<CreatureSpawnEvent.SpawnReason> SAFE_TO_CANCEL;

    static {
        Set<CreatureSpawnEvent.SpawnReason> safe = EnumSet.copyOf(BLOCKED_SPAWN_REASONS);
        safe.add(CreatureSpawnEvent.SpawnReason.SPAWNER_EGG);
        safe.add(CreatureSpawnEvent.SpawnReason.DISPENSE_EGG);
        safe.add(CreatureSpawnEvent.SpawnReason.COMMAND);
        SAFE_TO_CANCEL = safe;
    }

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
        if (player != null) {
            if (isDenied(player, Flags.FIRE_IGNITE, event.getBlock())) {
                event.setCancelled(true);
                sendDeniedMessage(player);
            }
            return;
        }
        // no player: lava ignition has its own flag, everything else stays fire-spread
        boolean allowed = event.getCause() == BlockIgniteEvent.IgniteCause.LAVA
                ? resolveOrGeneral(Flags.LAVA_FIRE, Flags.FIRE_SPREAD, event.getBlock())
                : !isDenied(Flags.FIRE_SPREAD, event.getBlock());
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onSpread(BlockSpreadEvent event) {
        Material formed = event.getNewState().getType();
        if (formed == Material.FIRE) {
            if (isDenied(Flags.FIRE_SPREAD, event.getBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        // a specific spread flag (grass, mycelium, vines, sculk) overrides the general block-spread
        Flag<Boolean> specific = specificSpreadFlag(formed);
        boolean allowed = specific != null
                ? resolveOrGeneral(specific, Flags.BLOCK_SPREAD, event.getBlock())
                : !isDenied(Flags.BLOCK_SPREAD, event.getBlock());
        if (!allowed) {
            event.setCancelled(true);
        }
    }

    private static Flag<Boolean> specificSpreadFlag(Material formed) {
        return switch (formed) {
            case GRASS_BLOCK -> Flags.GRASS_SPREAD;
            case MYCELIUM -> Flags.MYCELIUM_SPREAD;
            case VINE, WEEPING_VINES, WEEPING_VINES_PLANT, TWISTING_VINES, TWISTING_VINES_PLANT -> Flags.VINE_GROWTH;
            case SCULK, SCULK_VEIN -> Flags.SCULK_GROWTH;
            default -> null;
        };
    }

    @EventHandler(ignoreCancelled = true)
    public void onBurn(BlockBurnEvent event) {
        // a block being consumed by fire can be denied on its own (fire-burn), otherwise fire-spread
        if (!resolveOrGeneral(Flags.FIRE_BURN, Flags.FIRE_SPREAD, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFluidFlow(BlockFromToEvent event) {
        Material source = event.getBlock().getType();
        if (source != Material.WATER && source != Material.LAVA) {
            // non-fluid BlockFromToEvent (e.g. dragon egg) keeps the general fluid-flow behaviour
            if (isDenied(Flags.FLUID_FLOW, event.getToBlock())) {
                event.setCancelled(true);
            }
            return;
        }
        Flag<Boolean> specific = source == Material.LAVA ? Flags.LAVA_FLOW : Flags.WATER_FLOW;
        if (!resolveOrGeneral(specific, Flags.FLUID_FLOW, event.getToBlock())) {
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
    public void onLightning(LightningStrikeEvent event) {
        if (isDenied(Flags.LIGHTNING, event.getLightning().getLocation())) {
            event.setCancelled(true);
        }
    }

    /** Blocks forming naturally: snow layers and ice. */
    @EventHandler(ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Material formed = event.getNewState().getType();
        Flag<Boolean> flag;
        if (formed == Material.SNOW || formed == Material.SNOW_BLOCK) {
            flag = Flags.SNOW_FALL;
        } else if (formed == Material.ICE) {
            flag = Flags.ICE_FORM;
        } else {
            return;
        }
        if (isDenied(flag, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Blocks fading/reverting: melting snow/ice, drying farmland, dying coral. */
    @EventHandler(ignoreCancelled = true)
    public void onBlockFade(BlockFadeEvent event) {
        Material type = event.getBlock().getType();
        Flag<Boolean> flag;
        if (type == Material.SNOW || type == Material.SNOW_BLOCK) {
            flag = Flags.SNOW_MELT;
        } else if (type == Material.ICE) {
            flag = Flags.ICE_MELT;
        } else if (type == Material.FROSTED_ICE) {
            flag = Flags.FROSTED_ICE_MELT;
        } else if (type == Material.FARMLAND) {
            flag = Flags.SOIL_DRY;
        } else if (isCoral(type)) {
            flag = Flags.CORAL_FADE;
        } else {
            return;
        }
        if (isDenied(flag, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Blocks formed by an entity: frost-walker frosted ice and snow-golem trails. */
    @EventHandler(ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        Material formed = event.getNewState().getType();
        Flag<Boolean> flag;
        if (formed == Material.FROSTED_ICE) {
            flag = Flags.FROSTED_ICE_FORM;
        } else if (event.getEntity() instanceof Snowman
                && (formed == Material.SNOW || formed == Material.SNOW_BLOCK)) {
            flag = Flags.SNOWMAN_TRAILS;
        } else {
            return;
        }
        if (isDenied(flag, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        // the exploding entity picks a source-specific flag (creeper/tnt/…) overriding
        // the general entity-explosion wherever that specific flag is set
        Flag<Boolean> specific = explosionSourceFlag(event.getEntity());
        if (specific == null) {
            filterExplodedBlocks(event.blockList(), Flags.ENTITY_EXPLOSION);
            return;
        }
        event.blockList().removeIf(block -> !resolveOrGeneral(specific, Flags.ENTITY_EXPLOSION, block));
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        filterExplodedBlocks(event.blockList(), Flags.BLOCK_EXPLOSION);
    }

    @EventHandler(ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        if (isDenied(Flags.POTION_SPLASH, event.getPotion().getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        if (isDenied(Flags.POTION_SPLASH, event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
    }

    private static Flag<Boolean> explosionSourceFlag(Entity source) {
        if (source instanceof Creeper) return Flags.CREEPER_EXPLOSION;
        if (source instanceof TNTPrimed) return Flags.TNT;
        if (source instanceof Fireball) return Flags.GHAST_FIREBALL;
        if (source instanceof Wither || source instanceof WitherSkull) return Flags.WITHER_DAMAGE;
        if (source instanceof EnderDragon) return Flags.ENDERDRAGON_BLOCK_DAMAGE;
        return null;
    }

    @EventHandler(ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        CreatureSpawnEvent.SpawnReason reason = event.getSpawnReason();
        if (!SAFE_TO_CANCEL.contains(reason)) {
            return; // never cancel conversions (DROWNED, FROZEN…): the server deletes the source mob
        }
        Location location = event.getLocation();
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        RegionManager manager = this.plugin.getRegionManager();

        // deny-spawn: an explicit blocklist of entity types wins outright
        List<String> deniedTypes = manager.resolveFlag(world, x, y, z, Flags.DENY_SPAWN, null);
        if (!deniedTypes.isEmpty() && containsType(deniedTypes, event.getEntityType())) {
            event.setCancelled(true);
            return;
        }

        // most-specific → general: entity type, then category, then reason, then mob-spawning
        Boolean decision = null;
        for (Flag<Boolean> candidate : spawnCandidates(event)) {
            Optional<Boolean> value = manager.resolveFlagIfSet(world, x, y, z, candidate, null);
            if (value.isPresent()) {
                decision = value.get();
                break;
            }
        }
        if (decision == null && BLOCKED_SPAWN_REASONS.contains(reason)) {
            decision = manager.resolveFlag(world, x, y, z, Flags.MOB_SPAWNING, null);
        }
        if (decision != null && !decision) {
            event.setCancelled(true);
        }
    }

    /** The specific spawn flags applying to this spawn, most specific first. */
    private static List<Flag<Boolean>> spawnCandidates(CreatureSpawnEvent event) {
        List<Flag<Boolean>> candidates = new ArrayList<>(3);
        Entity entity = event.getEntity();
        if (entity instanceof Phantom) {
            candidates.add(Flags.PHANTOM_SPAWNING);
        }
        if (entity instanceof Slime) { // MagmaCube extends Slime
            candidates.add(Flags.SLIME_SPAWNING);
        }
        if (entity instanceof Animals) {
            candidates.add(Flags.ANIMAL_SPAWNING);
        } else if (entity instanceof Monster) {
            candidates.add(Flags.MONSTER_SPAWNING);
        }
        Flag<Boolean> reasonFlag = reasonSpawnFlag(event.getSpawnReason());
        if (reasonFlag != null) {
            candidates.add(reasonFlag);
        }
        return candidates;
    }

    private static Flag<Boolean> reasonSpawnFlag(CreatureSpawnEvent.SpawnReason reason) {
        return switch (reason) {
            case SPAWNER -> Flags.SPAWNER_SPAWNING;
            case NATURAL -> Flags.NATURAL_SPAWNING;
            case RAID, VILLAGE_INVASION, VILLAGE_DEFENSE -> Flags.RAID_SPAWNING;
            case PATROL -> Flags.PATROL_SPAWNING;
            case NETHER_PORTAL -> Flags.PORTAL_SPAWNING;
            case SPAWNER_EGG, DISPENSE_EGG -> Flags.EGG_SPAWNING;
            case COMMAND -> Flags.COMMAND_SPAWNING;
            default -> null;
        };
    }

    /** Whether {@code type} matches an entry (bare name or {@code minecraft:}-prefixed, case-insensitive). */
    private static boolean containsType(List<String> entries, EntityType type) {
        String name = type.name().toLowerCase(Locale.ROOT);
        for (String entry : entries) {
            String normalized = entry.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("minecraft:")) {
                normalized = normalized.substring("minecraft:".length());
            }
            if (normalized.equals(name)) {
                return true;
            }
        }
        return false;
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

    private boolean isDenied(Flag<Boolean> flag, Location location) {
        boolean allowed = this.plugin.getRegionManager().resolveFlag(location.getWorld().getName(),
                location.getX(), location.getY(), location.getZ(), flag, null);
        return !allowed;
    }

    /** general→specific override for environment flag families (e.g. water-flow over fluid-flow). */
    private boolean resolveOrGeneral(Flag<Boolean> specific, Flag<Boolean> general, Block block) {
        return this.plugin.getRegionManager().resolveFlagOrGeneral(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), specific, general, null);
    }

    private static boolean isCoral(Material type) {
        String name = type.name();
        return name.contains("CORAL") && !name.startsWith("DEAD");
    }

    private boolean isDenied(Player player, Flag<Boolean> flag, Block block) {
        if (player.hasPermission(this.plugin.getConfiguration().getBypassPermission())) return false;
        boolean allowed = this.plugin.getRegionManager().resolveFlag(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ(), flag, player.getUniqueId());
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
