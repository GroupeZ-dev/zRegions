package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * An ONLINE player, abstracted from the platform. This is the enrichment over
 * LuckPerms' model: zRegions needs the position and world of a player, LuckPerms
 * never does. Wrappers are produced by the platform's {@link RegionPlayerFactory}.
 */
public interface RegionPlayer {

    UUID getUniqueId();

    String getName();

    String getWorldName();

    RegionLocation getLocation();

    boolean hasPermission(String permission);

    void sendMessage(Component message);

    /** Shows a message in the player's action bar (above the hotbar). */
    void sendActionBar(Component message);

    /** Shows a title/subtitle pair with the platform's default fade timings. */
    void sendTitle(Component title, Component subtitle);

    void teleport(RegionLocation location);

    /**
     * Finds a safe standable spot at {@code target}'s column (solid ground with two
     * passable blocks above). Reads the world, so it must run on the game thread.
     * Empty when the target world is unloaded or the column has no safe spot.
     */
    Optional<RegionLocation> findSafeSpot(RegionLocation target);

    /**
     * Spawns a border-outline particle at the given coordinates in the player's
     * current world, visible to THIS player only.
     */
    void spawnBorderParticle(double x, double y, double z);

    /** Hands this player the selection wand item (platform-marked, rename-proof). */
    void giveWand();

    boolean isOnline();

    // --- player-state overrides (region flags: gamemode, time-lock, weather-lock, speeds) ---

    /** Client-only absolute time in ticks; {@link #resetPlayerTime()} restores server time. */
    void setPlayerTime(long ticks);

    void resetPlayerTime();

    /** Client-only weather; {@link #resetPlayerWeather()} restores server weather. */
    void setPlayerWeather(boolean rain);

    void resetPlayerWeather();

    void setWalkSpeed(float speed);

    void setFlySpeed(float speed);

    /** The player's game mode as a lowercase name (survival/creative/adventure/spectator). */
    String getGameMode();

    /** Sets the game mode from a name; an unknown name is ignored. */
    void setGameMode(String mode);

    double getHealth();

    void setHealth(double health);

    double getMaxHealth();

    int getFoodLevel();

    void setFoodLevel(int level);

    void setGlowing(boolean glowing);
}
