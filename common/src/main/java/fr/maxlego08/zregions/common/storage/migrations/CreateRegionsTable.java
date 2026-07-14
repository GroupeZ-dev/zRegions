package fr.maxlego08.zregions.common.storage.migrations;

import fr.maxlego08.sarah.database.Migration;

/**
 * Initial schema for the regions table. Tracked by class name in Sarah's migrations
 * table; declared with {@code createOrAlter} so columns added in future versions are
 * picked up automatically. Multi-server-ready from v1: {@code origin_server} scopes a
 * region to one server ("global" = every server) and {@code version} is the
 * optimistic-lock counter (see ARCHITECTURE.md §14.8).
 */
public final class CreateRegionsTable extends Migration {

    private final String tableName;

    public CreateRegionsTable(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public void up() {
        createOrAlter(this.tableName, schema -> {
            schema.uuid("id").primary();
            schema.string("name", 64);
            schema.string("world", 64);
            schema.integer("priority").defaultValue(0);
            schema.string("shape_type", 16);
            schema.longText("shape_data");
            schema.uuid("parent_id").nullable();
            schema.bool("is_global").defaultValue(false);
            // default value is injected verbatim into the DDL, hence the SQL quotes
            schema.string("origin_server", 64).defaultValue("'global'");
            schema.integer("version").defaultValue(0);
            schema.createdAt();
        });
    }
}
