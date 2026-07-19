package fr.maxlego08.zregions.common.state;

import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the persistent player-state flags (batch B9) as a player moves between
 * regions: {@code gamemode}, {@code time-lock}, {@code weather-lock},
 * {@code walk-speed}, {@code fly-speed}. Each override is applied on entering a
 * region that sets it (highest priority wins, resolved with the player's target)
 * and restored on leaving. The player's game mode from before the first override
 * is remembered so it can be restored precisely.
 *
 * <p>Runs on whatever thread the movement tracker calls it from — the player's
 * region thread on Folia, the main thread on Spigot — where player mutations are
 * safe. State that survives disconnect (speeds, game mode) is restored on quit;
 * client-only time/weather reset themselves when the connection drops.</p>
 */
public final class RegionPlayerStateService {

    private static final float DEFAULT_WALK_SPEED = 0.2f;
    private static final float DEFAULT_FLY_SPEED = 0.1f;

    private final ZRegionsPlugin plugin;
    private final Map<UUID, PlayerState> states = new ConcurrentHashMap<>();

    public RegionPlayerStateService(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    /** Recomputes and applies the overrides at the player's position (call on region-set change). */
    public void update(RegionPlayer player, RegionLocation location) {
        RegionManager manager = this.plugin.getRegionManager();
        String world = location.getWorldName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        UUID id = player.getUniqueId();
        PlayerState state = this.states.computeIfAbsent(id, key -> new PlayerState());

        applyGameMode(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.GAMEMODE, id));
        applyTime(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.TIME_LOCK, id));
        applyWeather(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.WEATHER_LOCK, id));
        applyWalkSpeed(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.WALK_SPEED, id));
        applyFlySpeed(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.FLY_SPEED, id));
        applyGlow(player, state, manager.resolveFlagIfSet(world, x, y, z, Flags.GLOW, id));
    }

    /** Restores every override and forgets the player (quit, disable). */
    public void clear(RegionPlayer player) {
        PlayerState state = this.states.remove(player.getUniqueId());
        if (state == null) {
            return;
        }
        if (state.timeApplied) {
            player.resetPlayerTime();
        }
        if (state.weatherApplied) {
            player.resetPlayerWeather();
        }
        if (state.walkApplied) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
        }
        if (state.flyApplied) {
            player.setFlySpeed(DEFAULT_FLY_SPEED);
        }
        if (state.glowApplied) {
            player.setGlowing(false);
        }
        if (state.originalGameMode != null) {
            player.setGameMode(state.originalGameMode);
        }
    }

    private void applyGameMode(RegionPlayer player, PlayerState state, Optional<String> desired) {
        if (desired.isPresent() && isGameMode(desired.get())) {
            String mode = desired.get().toLowerCase(Locale.ROOT);
            if (state.originalGameMode == null) {
                state.originalGameMode = player.getGameMode();
            }
            if (!mode.equals(state.appliedGameMode)) {
                player.setGameMode(mode);
                state.appliedGameMode = mode;
            }
        } else if (state.originalGameMode != null) {
            player.setGameMode(state.originalGameMode);
            state.originalGameMode = null;
            state.appliedGameMode = null;
        }
    }

    private void applyTime(RegionPlayer player, PlayerState state, Optional<String> desired) {
        OptionalLong ticks = desired.map(RegionPlayerStateService::parseTime).orElse(OptionalLong.empty());
        if (ticks.isPresent()) {
            long value = ticks.getAsLong();
            if (!state.timeApplied || state.appliedTime != value) {
                player.setPlayerTime(value);
                state.timeApplied = true;
                state.appliedTime = value;
            }
        } else if (state.timeApplied) {
            player.resetPlayerTime();
            state.timeApplied = false;
        }
    }

    private void applyWeather(RegionPlayer player, PlayerState state, Optional<String> desired) {
        Optional<Boolean> rain = desired.flatMap(RegionPlayerStateService::parseWeather);
        if (rain.isPresent()) {
            boolean value = rain.get();
            if (!state.weatherApplied || state.appliedRain != value) {
                player.setPlayerWeather(value);
                state.weatherApplied = true;
                state.appliedRain = value;
            }
        } else if (state.weatherApplied) {
            player.resetPlayerWeather();
            state.weatherApplied = false;
        }
    }

    private void applyWalkSpeed(RegionPlayer player, PlayerState state, Optional<Double> desired) {
        if (desired.isPresent()) {
            player.setWalkSpeed(desired.get().floatValue());
            state.walkApplied = true;
        } else if (state.walkApplied) {
            player.setWalkSpeed(DEFAULT_WALK_SPEED);
            state.walkApplied = false;
        }
    }

    private void applyFlySpeed(RegionPlayer player, PlayerState state, Optional<Double> desired) {
        if (desired.isPresent()) {
            player.setFlySpeed(desired.get().floatValue());
            state.flyApplied = true;
        } else if (state.flyApplied) {
            player.setFlySpeed(DEFAULT_FLY_SPEED);
            state.flyApplied = false;
        }
    }

    private void applyGlow(RegionPlayer player, PlayerState state, Optional<Boolean> desired) {
        if (desired.orElse(Boolean.FALSE)) {
            if (!state.glowApplied) {
                player.setGlowing(true);
                state.glowApplied = true;
            }
        } else if (state.glowApplied) {
            player.setGlowing(false);
            state.glowApplied = false;
        }
    }

    private static boolean isGameMode(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "survival", "creative", "adventure", "spectator" -> true;
            default -> false;
        };
    }

    /** A named time-of-day or a raw tick count. */
    private static OptionalLong parseTime(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "day" -> OptionalLong.of(1000);
            case "noon" -> OptionalLong.of(6000);
            case "sunset", "dusk" -> OptionalLong.of(12000);
            case "night" -> OptionalLong.of(13000);
            case "midnight" -> OptionalLong.of(18000);
            case "sunrise", "dawn" -> OptionalLong.of(23000);
            default -> {
                try {
                    yield OptionalLong.of(Long.parseLong(value.trim()));
                } catch (NumberFormatException exception) {
                    yield OptionalLong.empty();
                }
            }
        };
    }

    private static Optional<Boolean> parseWeather(String value) {
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "clear", "sun", "sunny" -> Optional.of(Boolean.FALSE);
            case "rain", "downfall", "storm", "snow" -> Optional.of(Boolean.TRUE);
            default -> Optional.empty();
        };
    }

    private static final class PlayerState {
        private String originalGameMode;
        private String appliedGameMode;
        private boolean timeApplied;
        private long appliedTime;
        private boolean weatherApplied;
        private boolean appliedRain;
        private boolean walkApplied;
        private boolean flyApplied;
        private boolean glowApplied;
    }
}
