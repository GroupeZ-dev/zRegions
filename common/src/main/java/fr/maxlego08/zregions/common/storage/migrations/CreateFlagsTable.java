package fr.maxlego08.zregions.common.storage.migrations;

import fr.maxlego08.sarah.database.Migration;

/**
 * Initial schema for the region flags table. One row per (region, flag, group target);
 * the value is stored as text and decoded by the flag registry. Rows cascade with their
 * region, but the storage layer also deletes them explicitly (SQLite does not always
 * enforce foreign keys).
 */
public final class CreateFlagsTable extends Migration {

    private final String tableName;
    private final String regionsTableName;

    public CreateFlagsTable(String tableName, String regionsTableName) {
        this.tableName = tableName;
        this.regionsTableName = regionsTableName;
    }

    @Override
    public void up() {
        createOrAlter(this.tableName, schema -> {
            schema.autoIncrement("id");
            schema.uuid("region_id").foreignKey(this.regionsTableName, "id", true);
            schema.string("flag_key", 64);
            schema.string("group_target", 32);
            schema.longText("flag_value");
        });
    }
}
