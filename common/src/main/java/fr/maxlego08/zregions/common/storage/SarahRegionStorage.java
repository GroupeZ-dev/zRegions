package fr.maxlego08.zregions.common.storage;

import fr.maxlego08.sarah.DatabaseConfiguration;
import fr.maxlego08.sarah.DatabaseConnection;
import fr.maxlego08.sarah.HikariDatabaseConnection;
import fr.maxlego08.sarah.MigrationManager;
import fr.maxlego08.sarah.RequestHelper;
import fr.maxlego08.sarah.SqliteConnection;
import fr.maxlego08.sarah.database.DatabaseType;
import fr.maxlego08.sarah.logger.Logger;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.storage.dto.FlagDTO;
import fr.maxlego08.zregions.common.storage.dto.MemberDTO;
import fr.maxlego08.zregions.common.storage.dto.RegionDTO;
import fr.maxlego08.zregions.common.storage.migrations.CreateFlagsTable;
import fr.maxlego08.zregions.common.storage.migrations.CreateMembersTable;
import fr.maxlego08.zregions.common.storage.migrations.CreateRegionsTable;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sarah-backed {@link RegionStorage} (SQLite for single-server, MySQL/MariaDB behind
 * HikariCP for networks). All methods are synchronous — callers schedule them on the
 * async pool. The shared database is the multi-server source of truth: each instance
 * only loads the regions owned by its server name plus the "global" ones.
 */
public final class SarahRegionStorage implements RegionStorage {

    private static final String SQLITE_FILE_NAME = "regions.db";

    private final ZRegionsConfiguration configuration;
    private final Path dataFolder;
    private final Logger logger;

    private final String regionsTable;
    private final String flagsTable;
    private final String membersTable;

    private DatabaseConnection connection;
    private RequestHelper requestHelper;

    public SarahRegionStorage(ZRegionsPlugin plugin) {
        this(plugin.getConfiguration(), plugin.getBootstrap().getDataDirectory(), plugin.getLogger());
    }

    public SarahRegionStorage(ZRegionsConfiguration configuration, Path dataFolder, PluginLogger logger) {
        this.configuration = configuration;
        this.dataFolder = dataFolder;
        this.logger = logger::info;

        String prefix = configuration.getTablePrefix();
        this.regionsTable = prefix + "regions";
        this.flagsTable = prefix + "flags";
        this.membersTable = prefix + "members";
    }

    @Override
    public void connect() throws Exception {
        DatabaseType type = parseType(this.configuration.getStorageType());

        DatabaseConfiguration databaseConfiguration;
        if (type == DatabaseType.SQLITE) {
            databaseConfiguration = new DatabaseConfiguration("", null, null, 0, null, null,
                    this.configuration.isDebug(), DatabaseType.SQLITE);
            SqliteConnection sqlite = new SqliteConnection(databaseConfiguration, this.dataFolder.toFile(), this.logger);
            sqlite.setFileName(SQLITE_FILE_NAME);
            this.connection = sqlite;
        } else {
            databaseConfiguration = new DatabaseConfiguration("",
                    this.configuration.getDatabaseUser(), this.configuration.getDatabasePassword(),
                    this.configuration.getDatabasePort(), this.configuration.getDatabaseHost(),
                    this.configuration.getDatabaseName(), this.configuration.isDebug(), type);
            this.connection = new HikariDatabaseConnection(databaseConfiguration, this.logger);
        }

        MigrationManager.setDatabaseConfiguration(databaseConfiguration);
        // keep connect() idempotent within one JVM (reload, tests): drop our stale registrations
        MigrationManager.getMigrations().removeIf(migration -> migration instanceof CreateRegionsTable
                || migration instanceof CreateFlagsTable || migration instanceof CreateMembersTable);
        MigrationManager.registerMigration(new CreateRegionsTable(this.regionsTable));
        MigrationManager.registerMigration(new CreateFlagsTable(this.flagsTable, this.regionsTable));
        MigrationManager.registerMigration(new CreateMembersTable(this.membersTable, this.regionsTable));
        MigrationManager.execute(this.connection, this.logger);

        this.requestHelper = new RequestHelper(this.connection, this.logger);
    }

    @Override
    public void disconnect() {
        if (this.connection != null) {
            this.connection.disconnect();
        }
    }

    @Override
    public List<StoredRegion> loadRegions(String serverName) {
        List<RegionDTO> rows = this.requestHelper.select(this.regionsTable, RegionDTO.class,
                schema -> schema.whereIn("origin_server", serverName, ZRegionsConfiguration.GLOBAL_SERVER));

        Map<UUID, StoredRegion> regions = new LinkedHashMap<>();
        for (RegionDTO row : rows) {
            regions.computeIfAbsent(row.getId(), id -> toStoredRegion(row));
        }
        return new ArrayList<>(regions.values());
    }

    @Override
    public Optional<StoredRegion> loadRegion(UUID id) {
        List<RegionDTO> rows = this.requestHelper.select(this.regionsTable, RegionDTO.class,
                schema -> schema.where("id", id));
        return rows.isEmpty() ? Optional.empty() : Optional.of(toStoredRegion(rows.get(0)));
    }

    @Override
    public void saveRegion(StoredRegion region) {
        this.requestHelper.upsert(this.regionsTable, schema -> {
            schema.uuid("id", region.id()).primary();
            schema.string("name", region.name());
            schema.string("world", region.world());
            schema.bigInt("priority", region.priority());
            schema.string("shape_type", region.shapeType());
            schema.string("shape_data", region.shapeData());
            schema.string("parent_id", region.parentId() == null ? null : region.parentId().toString());
            schema.bool("is_global", region.global());
            schema.string("origin_server", region.originServer());
            schema.bigInt("version", region.version());
        });
    }

    @Override
    public void deleteRegion(UUID id) {
        // children first and explicitly: SQLite does not always enforce foreign keys
        this.requestHelper.delete(this.flagsTable, schema -> schema.where("region_id", id));
        this.requestHelper.delete(this.membersTable, schema -> schema.where("region_id", id));
        this.requestHelper.delete(this.regionsTable, schema -> schema.where("id", id));
    }

    @Override
    public void saveFlag(UUID regionId, String flagKey, String groupTarget, String value) {
        deleteFlag(regionId, flagKey, groupTarget);
        this.requestHelper.insert(this.flagsTable, schema -> {
            schema.uuid("region_id", regionId);
            schema.string("flag_key", flagKey);
            schema.string("group_target", groupTarget);
            schema.string("flag_value", value);
        });
    }

    @Override
    public void deleteFlag(UUID regionId, String flagKey, String groupTarget) {
        this.requestHelper.delete(this.flagsTable, schema -> {
            schema.where("region_id", regionId);
            schema.where("flag_key", flagKey);
            schema.where("group_target", groupTarget);
        });
    }

    @Override
    public void saveMember(UUID regionId, UUID playerId, String role) {
        deleteMember(regionId, playerId);
        this.requestHelper.insert(this.membersTable, schema -> {
            schema.uuid("region_id", regionId);
            schema.uuid("player", playerId);
            schema.string("role", role);
        });
    }

    @Override
    public void deleteMember(UUID regionId, UUID playerId) {
        this.requestHelper.delete(this.membersTable, schema -> {
            schema.where("region_id", regionId);
            schema.where("player", playerId);
        });
    }

    private StoredRegion toStoredRegion(RegionDTO row) {
        return new StoredRegion(row.getId(), row.getName(), row.getWorld(), row.getPriority(),
                row.getShapeType(), row.getShapeData(), row.getParentId(), row.isGlobal(),
                row.getOriginServer(), row.getVersion(), loadFlags(row.getId()), loadMembers(row.getId()));
    }

    private List<StoredRegion.StoredFlag> loadFlags(UUID regionId) {
        List<FlagDTO> rows = this.requestHelper.select(this.flagsTable, FlagDTO.class,
                schema -> schema.where("region_id", regionId));
        List<StoredRegion.StoredFlag> flags = new ArrayList<>(rows.size());
        for (FlagDTO row : rows) {
            flags.add(new StoredRegion.StoredFlag(row.getFlagKey(), row.getGroupTarget(), row.getFlagValue()));
        }
        return flags;
    }

    private List<StoredRegion.StoredMember> loadMembers(UUID regionId) {
        List<MemberDTO> rows = this.requestHelper.select(this.membersTable, MemberDTO.class,
                schema -> schema.where("region_id", regionId));
        List<StoredRegion.StoredMember> members = new ArrayList<>(rows.size());
        for (MemberDTO row : rows) {
            members.add(new StoredRegion.StoredMember(row.getPlayer(), row.getRole()));
        }
        return members;
    }

    private static DatabaseType parseType(String value) {
        try {
            return DatabaseType.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return DatabaseType.SQLITE;
        }
    }
}
