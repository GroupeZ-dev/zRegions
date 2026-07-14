package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

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

    void teleport(RegionLocation location);

    boolean isOnline();
}
