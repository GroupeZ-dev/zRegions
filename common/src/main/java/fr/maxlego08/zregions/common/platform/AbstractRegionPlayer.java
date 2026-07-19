package fr.maxlego08.zregions.common.platform;

import net.kyori.adventure.text.Component;

import java.util.Optional;
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
    public Optional<RegionLocation> findSafeSpot(RegionLocation target) {
        return this.factory.findSafeSpot(target);
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
    public void setPlayerTime(long ticks) {
        this.factory.setPlayerTime(this.handle, ticks);
    }

    @Override
    public void resetPlayerTime() {
        this.factory.resetPlayerTime(this.handle);
    }

    @Override
    public void setPlayerWeather(boolean rain) {
        this.factory.setPlayerWeather(this.handle, rain);
    }

    @Override
    public void resetPlayerWeather() {
        this.factory.resetPlayerWeather(this.handle);
    }

    @Override
    public void setWalkSpeed(float speed) {
        this.factory.setWalkSpeed(this.handle, speed);
    }

    @Override
    public void setFlySpeed(float speed) {
        this.factory.setFlySpeed(this.handle, speed);
    }

    @Override
    public String getGameMode() {
        return this.factory.getGameMode(this.handle);
    }

    @Override
    public void setGameMode(String mode) {
        this.factory.setGameMode(this.handle, mode);
    }

    @Override
    public double getHealth() {
        return this.factory.getHealth(this.handle);
    }

    @Override
    public void setHealth(double health) {
        this.factory.setHealth(this.handle, health);
    }

    @Override
    public double getMaxHealth() {
        return this.factory.getMaxHealth(this.handle);
    }

    @Override
    public int getFoodLevel() {
        return this.factory.getFoodLevel(this.handle);
    }

    @Override
    public void setFoodLevel(int level) {
        this.factory.setFoodLevel(this.handle, level);
    }

    @Override
    public void setGlowing(boolean glowing) {
        this.factory.setGlowing(this.handle, glowing);
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
