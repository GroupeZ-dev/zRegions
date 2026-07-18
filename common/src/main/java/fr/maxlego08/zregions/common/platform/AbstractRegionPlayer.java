package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

import java.util.UUID;

/**
 * Single generic implementation of {@link RegionPlayer}, delegating everything to
 * the platform factory (LuckPerms' AbstractSender pattern applied to players).
 */
final class AbstractRegionPlayer<T> implements RegionPlayer {

    private final RegionPlayerFactory<T> factory;
    private final T handle;
    private final UUID uniqueId;
    private final String name;

    AbstractRegionPlayer(RegionPlayerFactory<T> factory, T handle) {
        this.factory = factory;
        this.handle = handle;
        this.uniqueId = factory.getUniqueId(handle);
        this.name = factory.getName(handle);
    }

    @Override
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getWorldName() {
        return this.factory.getLocation(this.handle).getWorldName();
    }

    @Override
    public RegionLocation getLocation() {
        return this.factory.getLocation(this.handle);
    }

    @Override
    public boolean hasPermission(String permission) {
        return this.factory.hasPermission(this.handle, permission);
    }

    @Override
    public void sendMessage(Component message) {
        this.factory.sendMessage(this.handle, message);
    }

    @Override
    public void sendActionBar(Component message) {
        this.factory.sendActionBar(this.handle, message);
    }

    @Override
    public void sendTitle(Component title, Component subtitle) {
        this.factory.sendTitle(this.handle, title, subtitle);
    }

    @Override
    public void teleport(RegionLocation location) {
        this.factory.teleport(this.handle, location);
    }

    @Override
    public void spawnBorderParticle(double x, double y, double z) {
        this.factory.spawnBorderParticle(this.handle, x, y, z);
    }

    @Override
    public void giveWand() {
        this.factory.giveWand(this.handle);
    }

    @Override
    public boolean isOnline() {
        return this.factory.isOnline(this.handle);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractRegionPlayer<?> other)) return false;
        return this.uniqueId.equals(other.uniqueId);
    }

    @Override
    public int hashCode() {
        return this.uniqueId.hashCode();
    }
}
