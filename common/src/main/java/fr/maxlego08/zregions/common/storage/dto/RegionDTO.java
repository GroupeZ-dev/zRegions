package fr.maxlego08.zregions.common.storage.dto;

import fr.maxlego08.sarah.Column;

import java.util.UUID;

/**
 * Row mapping for the regions table. Sarah maps result columns to the first
 * constructor's parameters in field-declaration order, so field order and
 * constructor order must stay in sync.
 */
public final class RegionDTO {

    @Column("id")
    private final UUID id;
    @Column("name")
    private final String name;
    @Column("world")
    private final String world;
    @Column("priority")
    private final int priority;
    @Column("shape_type")
    private final String shapeType;
    @Column("shape_data")
    private final String shapeData;
    @Column("parent_id")
    private final UUID parentId;
    @Column("is_global")
    private final boolean global;
    @Column("origin_server")
    private final String originServer;
    @Column("version")
    private final int version;

    public RegionDTO(UUID id, String name, String world, int priority, String shapeType,
                     String shapeData, UUID parentId, boolean global, String originServer, int version) {
        this.id = id;
        this.name = name;
        this.world = world;
        this.priority = priority;
        this.shapeType = shapeType;
        this.shapeData = shapeData;
        this.parentId = parentId;
        this.global = global;
        this.originServer = originServer;
        this.version = version;
    }

    public UUID getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getWorld() {
        return this.world;
    }

    public int getPriority() {
        return this.priority;
    }

    public String getShapeType() {
        return this.shapeType;
    }

    public String getShapeData() {
        return this.shapeData;
    }

    public UUID getParentId() {
        return this.parentId;
    }

    public boolean isGlobal() {
        return this.global;
    }

    public String getOriginServer() {
        return this.originServer;
    }

    public int getVersion() {
        return this.version;
    }
}
