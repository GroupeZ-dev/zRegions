package fr.maxlego08.zregions.hooks.placeholderapi;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The outgoing {@code %zregions_…%} PlaceholderAPI expansion — everything is read
 * from the online player's position through the public {@link RegionManager}, so
 * it never touches storage. Instantiated by reflection from
 * {@code setupPlatformHooks()} only when PlaceholderAPI is present and enabled;
 * this class must never be linked before that check.
 *
 * <p>Placeholders (all resolved at the requester's current location):</p>
 * <ul>
 *   <li>{@code %zregions_current_region%} — name of the highest-priority region here (empty if none)</li>
 *   <li>{@code %zregions_current_region_priority%} — its priority</li>
 *   <li>{@code %zregions_region_count%} — total number of regions</li>
 *   <li>{@code %zregions_region_count_world%} — regions in the player's world</li>
 *   <li>{@code %zregions_is_owner%} / {@code %zregions_is_member%} — {@code true}/{@code false} for the region here</li>
 *   <li>{@code %zregions_flag_<key>%} — effective value of a flag here for the player (e.g. {@code flag_pvp})</li>
 * </ul>
 */
public final class ZRegionsExpansion extends PlaceholderExpansion {

    private static final String NONE = "";
    private static final String FLAG_PREFIX = "flag_";

    private final ZRegionsPlugin plugin;

    public ZRegionsExpansion(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getIdentifier() {
        return "zregions";
    }

    @Override
    public String getAuthor() {
        return "Maxlego08";
    }

    @Override
    public String getVersion() {
        return this.plugin.getBootstrap().getVersion();
    }

    /** Survive a /papi reload — zRegions outlives PlaceholderAPI's own reloads. */
    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, String params) {
        if (player == null || params == null) {
            return NONE;
        }
        RegionManager manager = this.plugin.getRegionManager();
        String world = player.getWorld().getName();
        Location location = player.getLocation();
        String key = params.toLowerCase(Locale.ROOT);

        switch (key) {
            case "current_region":
                return here(manager, world, location).map(Region::getName).orElse(NONE);
            case "current_region_priority":
                return here(manager, world, location).map(region -> String.valueOf(region.getPriority())).orElse(NONE);
            case "region_count":
                return String.valueOf(manager.getRegions().size());
            case "region_count_world":
                return String.valueOf(manager.getRegions(world).size());
            case "is_owner":
                return bool(here(manager, world, location)
                        .map(region -> region.hasRole(player.getUniqueId(), MemberRole.OWNER)).orElse(false));
            case "is_member":
                return bool(here(manager, world, location)
                        .map(region -> region.hasRole(player.getUniqueId(), MemberRole.OWNER)
                                || region.hasRole(player.getUniqueId(), MemberRole.MEMBER)).orElse(false));
            default:
                if (key.startsWith(FLAG_PREFIX)) {
                    return flagValue(manager, world, location, player.getUniqueId(), key.substring(FLAG_PREFIX.length()));
                }
                return null; // unknown placeholder: PlaceholderAPI leaves it untouched
        }
    }

    private Optional<Region> here(RegionManager manager, String world, Location location) {
        return manager.getHighestRegionAt(world, location.getX(), location.getY(), location.getZ());
    }

    private String flagValue(RegionManager manager, String world, Location location, UUID playerId, String flagKey) {
        return this.plugin.getFlagRegistry().getFlag(flagKey)
                .map(flag -> resolveAndSerialize(manager, world, location, playerId, flag))
                .orElse(NONE);
    }

    // captures the flag's value type so serialize(value) type-checks
    private <T> String resolveAndSerialize(RegionManager manager, String world, Location location, UUID playerId, Flag<T> flag) {
        T value = manager.resolveFlag(world, location.getX(), location.getY(), location.getZ(), flag, playerId);
        return value == null ? NONE : flag.serialize(value);
    }

    private static String bool(boolean value) {
        return value ? "true" : "false";
    }
}
