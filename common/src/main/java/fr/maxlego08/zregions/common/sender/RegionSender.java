package fr.maxlego08.zregions.common.sender;

import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * A command source: a player OR the console (LuckPerms' Sender model — the console
 * is not a separate class, it is a sender with {@link #CONSOLE_UUID} and
 * {@link #isConsole()} true).
 */
public interface RegionSender {

    UUID CONSOLE_UUID = new UUID(0, 0);
    String CONSOLE_NAME = "Console";

    ZRegionsPlugin getPlugin();

    String getName();

    UUID getUniqueId();

    void sendMessage(Component message);

    boolean hasPermission(String permission);

    void performCommand(String commandLine);

    boolean isConsole();

    default boolean isValid() {
        return isConsole() || getPlugin().getBootstrap().isPlayerOnline(getUniqueId());
    }

    /** Present when this sender is an online player (grants access to position/world). */
    default Optional<RegionPlayer> asPlayer() {
        if (isConsole()) return Optional.empty();
        return getPlugin().getBootstrap().getPlayer(getUniqueId());
    }
}
