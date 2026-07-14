package fr.maxlego08.zregions.common.platform;

import java.util.Objects;

/**
 * Platform-agnostic world position (no Bukkit types). Immutable.
 */
public final class RegionLocation {

    private final String worldName;
    private final double x, y, z;
    private final float yaw, pitch;

    public RegionLocation(String worldName, double x, double y, double z) {
        this(worldName, x, y, z, 0f, 0f);
    }

    public RegionLocation(String worldName, double x, double y, double z, float yaw, float pitch) {
        this.worldName = Objects.requireNonNull(worldName, "worldName");
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public String getWorldName() {
        return this.worldName;
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public double getZ() {
        return this.z;
    }

    public float getYaw() {
        return this.yaw;
    }

    public float getPitch() {
        return this.pitch;
    }

    public int getBlockX() {
        return (int) Math.floor(this.x);
    }

    public int getBlockY() {
        return (int) Math.floor(this.y);
    }

    public int getBlockZ() {
        return (int) Math.floor(this.z);
    }

    @Override
    public String toString() {
        return this.worldName + "(" + getBlockX() + "," + getBlockY() + "," + getBlockZ() + ")";
    }
}
