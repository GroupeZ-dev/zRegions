package fr.maxlego08.zregions.common.plugin.bootstrap;

/**
 * The platforms zRegions can run on. Only BUKKIT ships in v1; the others exist
 * so common code never needs to change when a new bootstrap is added.
 */
public enum PlatformType {

    BUKKIT("Bukkit"),
    FABRIC("Fabric"),
    NUKKIT("Nukkit");

    private final String friendlyName;

    PlatformType(String friendlyName) {
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return this.friendlyName;
    }
}
