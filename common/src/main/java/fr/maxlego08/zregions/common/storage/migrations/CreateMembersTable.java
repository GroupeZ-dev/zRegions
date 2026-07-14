package fr.maxlego08.zregions.common.storage.migrations;

import fr.maxlego08.sarah.database.Migration;

/**
 * Initial schema for the region members table. One row per (region, player) with the
 * member's role name. Rows cascade with their region, but the storage layer also
 * deletes them explicitly (SQLite does not always enforce foreign keys).
 */
public final class CreateMembersTable extends Migration {

    private final String tableName;
    private final String regionsTableName;

    public CreateMembersTable(String tableName, String regionsTableName) {
        this.tableName = tableName;
        this.regionsTableName = regionsTableName;
    }

    @Override
    public void up() {
        createOrAlter(this.tableName, schema -> {
            schema.autoIncrement("id");
            schema.uuid("region_id").foreignKey(this.regionsTableName, "id", true);
            schema.uuid("player");
            schema.string("role", 16);
        });
    }
}
