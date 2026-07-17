package fr.maxlego08.zregions.common.region;

import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.region.RegionMember;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import fr.maxlego08.zregions.common.shape.ShapeCodec;
import fr.maxlego08.zregions.common.storage.StoredRegion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the mutation half of {@link ZRegionManager}: members, priority, parent
 * links (inheritance, instance swap, cycle guard), redefine and flag removal —
 * each checked against both the live cache and the persisted record (the fixture
 * scheduler runs "async" persistence on the calling thread).
 */
class RegionManagerMutationTest {

    private static final String WORLD = "world";

    private TestPluginFixture plugin;
    private ZRegionManager manager;

    @BeforeEach
    void setUp() {
        this.plugin = new TestPluginFixture();
        this.manager = new ZRegionManager(this.plugin);
        this.plugin.setRegionManager(this.manager);
    }

    @Test
    void setMemberAndRemoveMemberUpdateCacheAndStorage() {
        UUID playerId = UUID.randomUUID();
        Region region = this.manager.createRegion(WORLD, "claim", new CuboidShape(0, 0, 0, 10, 10, 10), 5, null);

        this.manager.setMember(region, playerId, MemberRole.MEMBER);
        assertTrue(region.getMembers().contains(new RegionMember(playerId, MemberRole.MEMBER)));
        StoredRegion stored = this.plugin.storage().loadRegion(region.getId()).orElseThrow();
        assertTrue(stored.members().contains(new StoredRegion.StoredMember(playerId, "MEMBER")));

        this.manager.removeMember(region, playerId);
        assertTrue(region.getMembers().isEmpty());
        stored = this.plugin.storage().loadRegion(region.getId()).orElseThrow();
        assertTrue(stored.members().isEmpty(), "the stored member row must be gone");
    }

    @Test
    void setPriorityUpdatesCacheAndStorage() {
        Region region = this.manager.createRegion(WORLD, "arena", new CuboidShape(0, 0, 0, 10, 10, 10), 5, null);

        this.manager.setPriority(region, 42);

        assertEquals(42, region.getPriority());
        assertEquals(42, this.plugin.storage().loadRegion(region.getId()).orElseThrow().priority());
    }

    @Test
    void setParentInheritsFlagsAndSwapsTheInstance() {
        // the parent lives elsewhere: the value can only come through inheritance
        Region parent = this.manager.createRegion(WORLD, "parent", new CuboidShape(500, 0, 500, 600, 100, 600), 5, null);
        Region child = this.manager.createRegion(WORLD, "child", new CuboidShape(0, 0, 0, 10, 10, 10), 10, null);
        this.manager.setFlag(parent, Flags.PVP, GroupTarget.ALL, false);

        Region updated = this.manager.setParent(child, parent);

        assertEquals(parent.getId(), updated.getParentId().orElseThrow());
        assertFalse(this.manager.resolveFlag(updated, Flags.PVP, null), "child inherits the parent deny");
        assertSame(updated, this.manager.getRegion(child.getId()).orElseThrow(), "the copy replaces the old instance");
        assertEquals(parent.getId(), this.plugin.storage().loadRegion(child.getId()).orElseThrow().parentId());

        Region cleared = this.manager.setParent(updated, null);
        assertTrue(cleared.getParentId().isEmpty());
        assertTrue(this.manager.resolveFlag(cleared, Flags.PVP, null), "back to the flag default without a parent");
    }

    @Test
    void setParentRejectsCycles() {
        Region a = this.manager.createRegion(WORLD, "a", new CuboidShape(0, 0, 0, 10, 10, 10), 5, null);
        Region b = this.manager.createRegion(WORLD, "b", new CuboidShape(100, 0, 100, 110, 10, 110), 5, null);
        Region childB = this.manager.setParent(b, a);

        assertThrows(IllegalArgumentException.class, () -> this.manager.setParent(a, childB),
                "A -> B -> A would loop");
        assertThrows(IllegalArgumentException.class, () -> this.manager.setParent(a, a),
                "a region cannot be its own parent");
    }

    @Test
    void redefineMovesTheRegionAndCarriesFlagsAndMembers() {
        UUID owner = UUID.randomUUID();
        Region region = this.manager.createRegion(WORLD, "shop", new CuboidShape(0, 0, 0, 10, 10, 10), 10, owner);
        this.manager.setFlag(region, Flags.PVP, GroupTarget.ALL, false);

        Region updated = this.manager.redefine(region, new CuboidShape(100, 0, 100, 110, 10, 110));

        List<Region> at = this.manager.getRegionsAt(WORLD, 105, 5, 105);
        assertEquals(1, at.size());
        assertSame(updated, at.get(0), "the index must serve the new instance at the new coords");
        assertTrue(this.manager.getRegionsAt(WORLD, 5, 5, 5).isEmpty(), "the old area must be free");
        assertFalse(this.manager.resolveFlag(updated, Flags.PVP, null), "flags carried over");
        assertTrue(updated.hasRole(owner, MemberRole.OWNER), "members carried over");
    }

    @Test
    void redefineGlobalRegionThrows() {
        this.plugin.storage().put(stored(UUID.randomUUID(), "__global__", cuboidData(0, 0, 0, 15, 15, 15), 0, null, true,
                List.of(), List.of()));
        this.manager.loadAllBlocking();
        Region global = this.manager.getRegion(WORLD, "__global__").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> this.manager.redefine(global, new CuboidShape(0, 0, 0, 10, 10, 10)));
    }

    @Test
    void removeFlagFallsBackToDefaultAndDeletesTheRow() {
        Region region = this.manager.createRegion(WORLD, "claim", new CuboidShape(0, 0, 0, 10, 10, 10), 5, null);
        this.manager.setFlag(region, Flags.PVP, GroupTarget.ALL, false);
        assertFalse(this.manager.resolveFlag(region, Flags.PVP, null));
        StoredRegion stored = this.plugin.storage().loadRegion(region.getId()).orElseThrow();
        assertTrue(stored.flags().contains(new StoredRegion.StoredFlag("pvp", "ALL", "deny")));

        this.manager.removeFlag(region, Flags.PVP, GroupTarget.ALL);

        assertTrue(this.manager.resolveFlag(region, Flags.PVP, null), "falls back to the flag default");
        stored = this.plugin.storage().loadRegion(region.getId()).orElseThrow();
        assertTrue(stored.flags().isEmpty(), "the stored flag row must be gone");
    }

    private static StoredRegion stored(UUID id, String name, String shapeData, int priority, UUID parentId,
                                       boolean global, List<StoredRegion.StoredFlag> flags,
                                       List<StoredRegion.StoredMember> members) {
        return new StoredRegion(id, name, WORLD, priority, ShapeType.CUBOID.name(), shapeData,
                parentId, global, "global", 0, flags, members);
    }

    private static String cuboidData(int x1, int y1, int z1, int x2, int y2, int z2) {
        return ShapeCodec.toJson(new CuboidShape(x1, y1, z1, x2, y2, z2));
    }
}
