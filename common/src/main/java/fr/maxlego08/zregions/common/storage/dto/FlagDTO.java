package fr.maxlego08.zregions.common.storage.dto;

import fr.maxlego08.sarah.Column;

/**
 * Row mapping for the region flags table (selected per region, so {@code region_id}
 * is not mapped). Field order must match the constructor's parameter order.
 */
public final class FlagDTO {

    @Column("flag_key")
    private final String flagKey;
    @Column("group_target")
    private final String groupTarget;
    @Column("flag_value")
    private final String flagValue;

    public FlagDTO(String flagKey, String groupTarget, String flagValue) {
        this.flagKey = flagKey;
        this.groupTarget = groupTarget;
        this.flagValue = flagValue;
    }

    public String getFlagKey() {
        return this.flagKey;
    }

    public String getGroupTarget() {
        return this.groupTarget;
    }

    public String getFlagValue() {
        return this.flagValue;
    }
}
