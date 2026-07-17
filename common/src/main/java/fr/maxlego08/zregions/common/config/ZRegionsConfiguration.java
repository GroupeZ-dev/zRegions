package fr.maxlego08.zregions.common.config;

import java.util.Locale;

/**
 * Typed view over config.yml. Multi-server-ready from v1: the {@code server} name
 * feeds the regions' {@code origin_server} column (see ARCHITECTURE.md §14).
 *
 * <p>Values read on the protection hot path (bypass permission, deny-message
 * throttle) are cached in volatile fields and refreshed on {@link #reload()} —
 * a YAML lookup per BlockBreakEvent would be waste.</p>
 */
public final class ZRegionsConfiguration {

    public static final String GLOBAL_SERVER = "global";
    public static final String DEFAULT_BYPASS_PERMISSION = "zregions.bypass";
    public static final long DEFAULT_DENY_THROTTLE_MILLIS = 2000L;

    private final ConfigurationAdapter adapter;

    private volatile String bypassPermission = DEFAULT_BYPASS_PERMISSION;
    private volatile long denyMessageThrottleMillis = DEFAULT_DENY_THROTTLE_MILLIS;

    public ZRegionsConfiguration(ConfigurationAdapter adapter) {
        this.adapter = adapter;
        refreshCachedValues();
    }

    public void reload() {
        this.adapter.reload();
        refreshCachedValues();
    }

    private void refreshCachedValues() {
        this.bypassPermission = this.adapter.getString("permissions.bypass", DEFAULT_BYPASS_PERMISSION);
        this.denyMessageThrottleMillis = Math.max(0,
                this.adapter.getInt("messages.deny-throttle-milliseconds", (int) DEFAULT_DENY_THROTTLE_MILLIS));
    }

    public String getLanguage() {
        return this.adapter.getString("language", "en").toLowerCase(Locale.ROOT);
    }

    /** The permission node bypassing every protection. HOT PATH — cached. */
    public String getBypassPermission() {
        return this.bypassPermission;
    }

    /** Minimum delay between two "denied" messages to the same player. HOT PATH — cached. */
    public long getDenyMessageThrottleMillis() {
        return this.denyMessageThrottleMillis;
    }

    // --- border display (/rg show) — read once per command, no caching needed ---

    /** Bukkit Particle name used to outline regions; parsed by the platform. */
    public String getBorderParticle() {
        return this.adapter.getString("borders.particle", "FLAME");
    }

    public int getBorderDisplaySeconds() {
        return Math.max(1, this.adapter.getInt("borders.display-seconds", 10));
    }

    public long getBorderRefreshMillis() {
        return Math.max(100, this.adapter.getInt("borders.refresh-milliseconds", 500));
    }

    public double getBorderPointSpacing() {
        return Math.max(0.1, this.adapter.getDouble("borders.point-spacing", 0.5));
    }

    /** Hard cap on particles per refresh — huge regions get a sparser outline, never a lag spike. */
    public int getBorderMaxPoints() {
        return Math.max(50, this.adapter.getInt("borders.max-points", 1500));
    }

    /** The logical name of THIS server in a network; "global" in single-server mode. */
    public String getServerName() {
        return this.adapter.getString("multi-server.server", GLOBAL_SERVER).toLowerCase(Locale.ROOT);
    }

    public boolean isMultiServerEnabled() {
        return this.adapter.getBoolean("multi-server.enabled", false);
    }

    public boolean isDebug() {
        return this.adapter.getBoolean("debug", false);
    }

    // --- storage ---

    public String getStorageType() {
        return this.adapter.getString("storage.type", "SQLITE").toUpperCase(Locale.ROOT);
    }

    public String getTablePrefix() {
        return this.adapter.getString("storage.table-prefix", "zregions_");
    }

    public String getDatabaseHost() {
        return this.adapter.getString("storage.database.host", "127.0.0.1");
    }

    public int getDatabasePort() {
        return this.adapter.getInt("storage.database.port", 3306);
    }

    public String getDatabaseName() {
        return this.adapter.getString("storage.database.database", "zregions");
    }

    public String getDatabaseUser() {
        return this.adapter.getString("storage.database.user", "root");
    }

    public String getDatabasePassword() {
        return this.adapter.getString("storage.database.password", "");
    }
}
