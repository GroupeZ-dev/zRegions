package fr.maxlego08.zregions.bukkit;

import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayerFactory;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Wraps Bukkit {@link Player}s into platform-agnostic RegionPlayers, exposing
 * the position/world data the region engine needs.
 */
public final class BukkitPlayerFactory extends RegionPlayerFactory<Player> {

    /** PDC marker identifying the selection wand — rename-proof, survives restarts. */
    public static final NamespacedKey WAND_KEY = new NamespacedKey("zregions", "wand");

    private final ZRegionsPlugin plugin;
    private final BukkitAudiences audiences;

    /** The parsed border particle, cached per config value (parsed once, not per point). */
    private volatile String cachedParticleName;
    private volatile Particle cachedParticle = Particle.FLAME;

    public BukkitPlayerFactory(ZRegionsPlugin plugin, BukkitAudiences audiences) {
        this.plugin = plugin;
        this.audiences = audiences;
    }

    @Override
    protected UUID getUniqueId(Player player) {
        return player.getUniqueId();
    }

    @Override
    protected String getName(Player player) {
        return player.getName();
    }

    @Override
    protected RegionLocation getLocation(Player player) {
        Location location = player.getLocation();
        return new RegionLocation(player.getWorld().getName(), location.getX(), location.getY(),
                location.getZ(), location.getYaw(), location.getPitch());
    }

    @Override
    protected boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permission);
    }

    @Override
    protected void sendMessage(Player player, Component message) {
        this.audiences.sender(player).sendMessage(message);
    }

    // Action bars and titles go through the Audience too — adventure-platform
    // does the packet work, so this stays Spigot-compatible.
    @Override
    protected void sendActionBar(Player player, Component message) {
        this.audiences.sender(player).sendActionBar(message);
    }

    @Override
    protected void sendTitle(Player player, Component title, Component subtitle) {
        this.audiences.sender(player).showTitle(Title.title(title, subtitle));
    }

    @Override
    protected void teleport(Player player, RegionLocation location) {
        World world = Bukkit.getWorld(location.getWorldName());
        if (world == null) {
            Bukkit.getLogger().warning("[zRegions] Unable to teleport " + player.getName()
                    + ": world " + location.getWorldName() + " is not loaded.");
            return;
        }
        player.teleport(new Location(world, location.getX(), location.getY(), location.getZ(),
                location.getYaw(), location.getPitch()));
    }

    /**
     * Scans the target column for a standable spot: solid ground with two passable,
     * non-liquid blocks above. Tries the region-centre height first, then the world
     * top. Reads blocks, so callers must run this on the game thread.
     */
    @Override
    protected Optional<RegionLocation> findSafeSpot(RegionLocation target) {
        World world = Bukkit.getWorld(target.getWorldName());
        if (world == null) {
            return Optional.empty();
        }
        int x = target.getBlockX();
        int z = target.getBlockZ();
        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight() - 1;
        int start = Math.max(minY + 1, Math.min(maxY - 1, target.getBlockY()));

        Optional<Integer> found = scanDown(world, x, z, start, minY);
        if (found.isEmpty()) {
            found = scanDown(world, x, z, maxY - 1, minY);
        }
        return found.map(safeY -> new RegionLocation(world.getName(), x + 0.5, safeY, z + 0.5,
                target.getYaw(), target.getPitch()));
    }

    /** First Y (scanning downward) with solid ground below and two empty blocks above. */
    private static Optional<Integer> scanDown(World world, int x, int z, int fromY, int minY) {
        for (int y = fromY; y > minY; y--) {
            Block ground = world.getBlockAt(x, y - 1, z);
            Block feet = world.getBlockAt(x, y, z);
            Block head = world.getBlockAt(x, y + 1, z);
            if (ground.getType().isSolid() && isEmptySpace(feet) && isEmptySpace(head)) {
                return Optional.of(y);
            }
        }
        return Optional.empty();
    }

    private static boolean isEmptySpace(Block block) {
        return !block.getType().isSolid() && !block.isLiquid();
    }

    /** {@link Player#spawnParticle} sends per-player packets — nobody else sees the outline. */
    @Override
    protected void spawnBorderParticle(Player player, double x, double y, double z) {
        player.spawnParticle(resolveParticle(), x, y, z, 1, 0, 0, 0, 0);
    }

    private Particle resolveParticle() {
        String name = this.plugin.getConfiguration().getBorderParticle();
        if (!name.equals(this.cachedParticleName)) {
            Particle parsed;
            try {
                parsed = Particle.valueOf(name.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                this.plugin.getLogger().warn("Unknown border particle '" + name + "', using FLAME.");
                parsed = Particle.FLAME;
            }
            this.cachedParticle = parsed;
            this.cachedParticleName = name;
        }
        return this.cachedParticle;
    }

    @Override
    protected void giveWand(Player player) {
        Material material;
        String configured = this.plugin.getConfiguration().getWandItem();
        try {
            material = Material.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            this.plugin.getLogger().warn("Unknown wand item '" + configured + "', using BLAZE_ROD.");
            material = Material.BLAZE_ROD;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(LegacyComponentSerializer.legacySection()
                    .serialize(this.plugin.getMessages().format(Message.WAND_NAME)));
            meta.getPersistentDataContainer().set(WAND_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        player.getInventory().addItem(item);
    }

    @Override
    protected boolean isOnline(Player player) {
        return player.isOnline();
    }

    // --- player-state overrides (region flags) ---

    @Override
    protected void setPlayerTime(Player player, long ticks) {
        player.setPlayerTime(ticks, false); // absolute, not relative to the world time
    }

    @Override
    protected void resetPlayerTime(Player player) {
        player.resetPlayerTime();
    }

    @Override
    protected void setPlayerWeather(Player player, boolean rain) {
        player.setPlayerWeather(rain ? WeatherType.DOWNFALL : WeatherType.CLEAR);
    }

    @Override
    protected void resetPlayerWeather(Player player) {
        player.resetPlayerWeather();
    }

    @Override
    protected void setWalkSpeed(Player player, float speed) {
        player.setWalkSpeed(clampSpeed(speed));
    }

    @Override
    protected void setFlySpeed(Player player, float speed) {
        player.setFlySpeed(clampSpeed(speed));
    }

    /** Bukkit rejects speeds outside [-1, 1]. */
    private static float clampSpeed(float speed) {
        return Math.max(-1f, Math.min(1f, speed));
    }

    @Override
    protected String getGameMode(Player player) {
        return player.getGameMode().name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void setGameMode(Player player, String mode) {
        try {
            player.setGameMode(GameMode.valueOf(mode.toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException exception) {
            // unknown game-mode name — ignore, the flag value was invalid
        }
    }

    @Override
    protected double getHealth(Player player) {
        return player.getHealth();
    }

    @Override
    protected void setHealth(Player player, double health) {
        player.setHealth(Math.max(0.0, Math.min(health, getMaxHealth(player))));
    }

    @Override
    protected double getMaxHealth(Player player) {
        AttributeInstance attribute = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        return attribute != null ? attribute.getValue() : 20.0;
    }

    @Override
    protected int getFoodLevel(Player player) {
        return player.getFoodLevel();
    }

    @Override
    protected void setFoodLevel(Player player, int level) {
        player.setFoodLevel(Math.max(0, Math.min(level, 20)));
    }

    @Override
    protected void setGlowing(Player player, boolean glowing) {
        player.setGlowing(glowing);
    }
}
