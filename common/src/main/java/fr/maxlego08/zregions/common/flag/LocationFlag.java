package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.common.platform.RegionLocation;

import java.util.Objects;
import java.util.Optional;

/**
 * A world-position flag (destination for {@code teleport}/{@code spawn}). The
 * value is the platform-agnostic {@link RegionLocation}.
 *
 * <p>It serialises to a stable, locale-independent {@code world;x;y;z;yaw;pitch}
 * string that re-parses exactly — the DB load path depends on the round-trip, so
 * coordinates use {@link Double#toString}/{@link Float#toString} (always a dot,
 * never a locale comma), like {@link DoubleFlag}. The world name comes first and
 * may itself contain the {@code ;} separator: parsing takes the last five fields
 * as the numbers and re-joins everything before them as the world, so unusual
 * world names still round-trip.</p>
 *
 * <p>The default is {@code null} (no location): a location only makes sense once
 * a region defines it, so callers resolve it with {@code resolveFlagIfSet} and
 * act only when present. {@link #serialize} is null-safe (returns an empty
 * string) so the flag editor can render the "unset" default.</p>
 */
public final class LocationFlag implements Flag<RegionLocation> {

    private static final char SEPARATOR = ';';
    private static final int FIELDS = 6;

    private final String key;

    public LocationFlag(String key) {
        this.key = Objects.requireNonNull(key, "key");
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public RegionLocation getDefaultValue() {
        return null;
    }

    @Override
    public Optional<RegionLocation> parse(String input) {
        if (input == null || input.isEmpty()) {
            return Optional.empty();
        }
        String[] parts = input.split(String.valueOf(SEPARATOR), -1);
        if (parts.length < FIELDS) {
            return Optional.empty();
        }
        int firstNumber = parts.length - 5;
        StringBuilder world = new StringBuilder(parts[0]);
        for (int i = 1; i < firstNumber; i++) {
            world.append(SEPARATOR).append(parts[i]);
        }
        if (world.length() == 0) {
            return Optional.empty();
        }
        try {
            double x = Double.parseDouble(parts[firstNumber]);
            double y = Double.parseDouble(parts[firstNumber + 1]);
            double z = Double.parseDouble(parts[firstNumber + 2]);
            float yaw = Float.parseFloat(parts[firstNumber + 3]);
            float pitch = Float.parseFloat(parts[firstNumber + 4]);
            if (!isFinite(x) || !isFinite(y) || !isFinite(z)
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch)) {
                return Optional.empty();
            }
            return Optional.of(new RegionLocation(world.toString(), x, y, z, yaw, pitch));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    @Override
    public String serialize(RegionLocation value) {
        if (value == null) {
            return "";
        }
        return value.getWorldName() + SEPARATOR + Double.toString(value.getX())
                + SEPARATOR + Double.toString(value.getY())
                + SEPARATOR + Double.toString(value.getZ())
                + SEPARATOR + Float.toString(value.getYaw())
                + SEPARATOR + Float.toString(value.getPitch());
    }

    @Override
    public String toString() {
        return "LocationFlag{" + this.key + "}";
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
