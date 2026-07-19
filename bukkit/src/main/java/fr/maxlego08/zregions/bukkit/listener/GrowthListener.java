package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.flag.Flags;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockFertilizeEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.bukkit.event.world.StructureGrowEvent;

/**
 * Enforces the natural-growth flags (batch B4): crop/plant growth, tree and
 * mushroom structure growth, bone meal and entity transformations. All silent —
 * these events have no player behind them (bone meal is environmental in spirit).
 * Block <em>spreading</em> (grass, mycelium, vines, sculk) stays in
 * {@link EnvironmentProtectionListener} because it shares BlockSpreadEvent with fire.
 */
public final class GrowthListener implements Listener {

    private final ZRegionsBukkitPlugin plugin;

    public GrowthListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    /** Crops, sugar cane, cactus, bamboo, stems, cave vines, kelp… */
    @EventHandler(ignoreCancelled = true)
    public void onGrow(BlockGrowEvent event) {
        if (isDenied(Flags.CROP_GROWTH, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    /** Saplings → trees and mushrooms → huge mushrooms (also bonemeal-forced growth). */
    @EventHandler(ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        TreeType species = event.getSpecies();
        boolean mushroom = species == TreeType.RED_MUSHROOM || species == TreeType.BROWN_MUSHROOM;
        Flag<Boolean> flag = mushroom ? Flags.MUSHROOM_GROWTH : Flags.TREE_GROWTH;
        if (isDenied(flag, event.getLocation())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFertilize(BlockFertilizeEvent event) {
        if (isDenied(Flags.BONE_MEAL, event.getBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onTransform(EntityTransformEvent event) {
        if (isDenied(Flags.ENTITY_TRANSFORM, event.getEntity().getLocation())) {
            event.setCancelled(true);
        }
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
}
