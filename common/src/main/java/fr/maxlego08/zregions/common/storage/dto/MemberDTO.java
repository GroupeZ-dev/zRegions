package fr.maxlego08.zregions.common.storage.dto;

import fr.maxlego08.sarah.Column;

import java.util.UUID;

/**
 * Row mapping for the region members table (selected per region, so {@code region_id}
 * is not mapped). Field order must match the constructor's parameter order.
 */
public final class MemberDTO {

    @Column("player")
    private final UUID player;
    @Column("role")
    private final String role;

    public MemberDTO(UUID player, String role) {
        this.player = player;
        this.role = role;
    }

    public UUID getPlayer() {
        return this.player;
    }

    public String getRole() {
        return this.role;
    }
}
