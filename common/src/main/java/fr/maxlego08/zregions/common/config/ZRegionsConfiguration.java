package fr.maxlego08.zregions.common.config;

import java.util.Locale;

/**
 * Typed view over config.yml. Multi-server-ready from v1: the {@code server} name
 * feeds the regions' {@code origin_server} column (see ARCHITECTURE.md §14).
 */
public final class ZRegionsConfiguration {

    public static final String GLOBAL_SERVER = "global";

    private final ConfigurationAdapter adapter;

    public ZRegionsConfiguration(ConfigurationAdapter adapter) {
        this.adapter = adapter;
    }

    public void reload() {
        this.adapter.reload();
    }

    public String getLanguage() {
        return this.adapter.getString("language", "en").toLowerCase(Locale.ROOT);
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
