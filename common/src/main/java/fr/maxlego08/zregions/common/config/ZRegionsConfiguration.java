package fr.maxlego08.zregions.common.config;

import fr.maxlego08.zregions.common.locale.Palette;
import net.kyori.adventure.text.format.TextColor;

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
    private volatile Palette palette = Palette.DEFAULT;

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
        this.palette = buildPalette();
    }

    /**
     * The configured message colour palette (built once per reload, since it is on
     * the render path). Each {@code messages.palette.*} colour is a hex string
     * ({@code #RRGGBB}, the {@code #} optional); an unset or invalid entry falls back
     * to its built-in default, so a typo never blanks the messages.
     */
    public Palette getPalette() {
        return this.palette;
    }

    private Palette buildPalette() {
        return new Palette(
                paletteColor("primary", Palette.PRIMARY),
                paletteColor("accent", Palette.ACCENT),
                paletteColor("success", Palette.SUCCESS),
                paletteColor("error", Palette.ERROR),
                paletteColor("body", Palette.BODY),
                paletteColor("muted", Palette.MUTED));
    }

    private TextColor paletteColor(String role, TextColor def) {
        String value = this.adapter.getString("messages.palette." + role, "").trim();
        String hex = value.startsWith("#") ? value.substring(1) : value;
        // strictly #RRGGBB: reject shorthand (#fff would parse to a surprising colour,
        // not expand CSS-style) and any other length, so a typo predictably keeps the default
        if (hex.length() != 6) {
            return def;
        }
        TextColor parsed = TextColor.fromHexString("#" + hex);
        return parsed != null ? parsed : def;
    }

    /** The permission node bypassing every protection. HOT PATH — cached. */
    public String getBypassPermission() {
        return this.bypassPermission;
    }

    /** Minimum delay between two "denied" messages to the same player. HOT PATH — cached. */
    public long getDenyMessageThrottleMillis() {
        return this.denyMessageThrottleMillis;
    }

    /** Bukkit Material name of the selection wand; parsed by the platform. */
    public String getWandItem() {
        return this.adapter.getString("selection.wand-item", "BLAZE_ROD");
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

    // --- region behaviour (read at command time, not hot path) ---

    /**
     * Whether {@code /rg create} adds the creator as the region's owner (default
     * {@code true}). When {@code false}, new regions have no owner and are only
     * manageable by an admin ({@code zregions.admin}).
     */
    public boolean isCreatorBecomesOwner() {
        return this.adapter.getBoolean("regions.creator-becomes-owner", true);
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
