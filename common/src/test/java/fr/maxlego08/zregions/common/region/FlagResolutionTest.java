package fr.maxlego08.zregions.common.region;

import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the whole effective-flag resolution chain of {@link ZRegionManager}:
 * priority walk, group targets, parent inheritance, global fallback, flag
 * defaults, and cache consistency on create/reload.
 */
class FlagResolutionTest {

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
    void highestPriorityRegionWinsOnOverlap() {
        Region outer = this.manager.createRegion(WORLD, "outer", new CuboidShape(0, 0, 0, 100, 100, 100), 10, null);
        Region inner = this.manager.createRegion(WORLD, "inner", new CuboidShape(10, 10, 10, 20, 20, 20), 20, null);
        this.manager.setFlag(outer, Flags.PVP, GroupTarget.ALL, true);
        this.manager.setFlag(inner, Flags.PVP, GroupTarget.ALL, false);

        assertFalse(this.manager.resolveFlag(WORLD, 15, 15, 15, Flags.PVP, null), "inner (priority 20) deny must win");
        assertTrue(this.manager.resolveFlag(WORLD, 50, 50, 50, Flags.PVP, null), "outside inner, outer allow applies");

        List<Region> at = this.manager.getRegionsAt(WORLD, 15, 15, 15);
        assertEquals(2, at.size());
        assertEquals(inner.getId(), at.get(0).getId(), "getRegionsAt must be sorted by priority desc");
    }

    @Test
    void groupTargetIsMoreSpecificThanAll() {
        UUID owner = UUID.randomUUID();
        Region region = this.manager.createRegion(WORLD, "claim", new CuboidShape(0, 0, 0, 30, 30, 30), 5, owner);
        assertTrue(region.hasRole(owner, MemberRole.OWNER));

        this.manager.setFlag(region, Flags.BLOCK_BREAK, GroupTarget.OWNER, true);
        this.manager.setFlag(region, Flags.BLOCK_BREAK, GroupTarget.ALL, false);

        assertTrue(this.manager.resolveFlag(WORLD, 5, 5, 5, Flags.BLOCK_BREAK, owner));
        assertFalse(this.manager.resolveFlag(WORLD, 5, 5, 5, Flags.BLOCK_BREAK, UUID.randomUUID()));
        assertFalse(this.manager.resolveFlag(WORLD, 5, 5, 5, Flags.BLOCK_BREAK, null), "null player is a visitor");
    }

    @Test
    void childInheritsFlagFromParent() {
        UUID parentId = UUID.randomUUID();
        // the parent lives elsewhere: the value can only come through inheritance
        this.plugin.storage().put(stored(parentId, "parent", cuboidData(500, 0, 500, 600, 100, 600), 5, null, false,
                List.of(new StoredRegion.StoredFlag("pvp", "ALL", "deny")), List.of()));
        this.plugin.storage().put(stored(UUID.randomUUID(), "child", cuboidData(10, 10, 10, 20, 20, 20), 20, parentId, false,
                List.of(), List.of()));
        this.manager.loadAllBlocking();

        assertFalse(this.manager.resolveFlag(WORLD, 15, 15, 15, Flags.PVP, null));
        assertTrue(this.manager.resolveFlag(WORLD, 15, 15, 15, Flags.BLOCK_PLACE, null), "unrelated flag keeps its default");
    }

    @Test
    void globalRegionIsTheFallbackWhenNoRegionDefinesTheFlag() {
        this.plugin.storage().put(stored(UUID.randomUUID(), "__global__", cuboidData(0, 0, 0, 15, 15, 15), 0, null, true,
                List.of(new StoredRegion.StoredFlag("interact", "ALL", "deny")), List.of()));
        this.manager.loadAllBlocking();

        assertFalse(this.manager.resolveFlag(WORLD, 400, 64, 400, Flags.INTERACT, null), "no region here: global applies");

        Region region = this.manager.createRegion(WORLD, "shop", new CuboidShape(0, 0, 0, 10, 10, 10), 10, null);
        assertFalse(this.manager.resolveFlag(WORLD, 5, 5, 5, Flags.INTERACT, null), "region without the flag falls through to global");
        this.manager.setFlag(region, Flags.INTERACT, GroupTarget.ALL, true);
        assertTrue(this.manager.resolveFlag(WORLD, 5, 5, 5, Flags.INTERACT, null), "a region value overrides global");
    }

    @Test
    void flagDefaultAppliesWhenNothingIsDefined() {
        assertTrue(this.manager.resolveFlag(WORLD, 0, 64, 0, Flags.CONTAINER_ACCESS, null));

        this.manager.createRegion(WORLD, "empty", new CuboidShape(0, 0, 0, 10, 100, 10), 10, null);
        assertTrue(this.manager.resolveFlag(WORLD, 5, 64, 5, Flags.CONTAINER_ACCESS, null), "region without values keeps the default");
    }

    @Test
    void createRegionRejectsDuplicateNamePerWorld() {
        this.manager.createRegion(WORLD, "spawn", new CuboidShape(0, 0, 0, 10, 10, 10), 1, null);

        assertThrows(IllegalArgumentException.class,
                () -> this.manager.createRegion(WORLD, "Spawn", new CuboidShape(50, 0, 50, 60, 10, 60), 2, null),
                "region names are case-insensitive per world");
        // same name in another world is fine
        this.manager.createRegion("world_nether", "spawn", new CuboidShape(0, 0, 0, 10, 10, 10), 1, null);
    }

    @Test
    void reloadPicksUpStorageChanges() {
        Region region = this.manager.createRegion(WORLD, "arena", new CuboidShape(0, 0, 0, 50, 50, 50), 10, null);
        assertTrue(this.manager.resolveFlag(WORLD, 25, 25, 25, Flags.PVP, null));

        // another server denies pvp: only the shared database knows
        this.plugin.storage().saveFlag(region.getId(), "pvp", "ALL", "deny");
        assertTrue(this.manager.resolveFlag(WORLD, 25, 25, 25, Flags.PVP, null), "cache untouched until reload");

        this.manager.reload(region.getId()).join();
        assertFalse(this.manager.resolveFlag(WORLD, 25, 25, 25, Flags.PVP, null));
        assertTrue(this.manager.getRegion(WORLD, "arena").isPresent(), "name lookup survives the swap");
        assertTrue(this.manager.getRegion(region.getId()).isPresent());
    }

    @Test
    void reloadRemovesRegionDeletedFromStorage() {
        Region region = this.manager.createRegion(WORLD, "temp", new CuboidShape(0, 0, 0, 20, 20, 20), 10, null);
        this.plugin.storage().deleteRegion(region.getId());

        this.manager.reload(region.getId()).join();

        assertTrue(this.manager.getRegion(region.getId()).isEmpty());
        assertTrue(this.manager.getRegion(WORLD, "temp").isEmpty());
        assertTrue(this.manager.getRegionsAt(WORLD, 10, 10, 10).isEmpty(), "the index must forget the region");
    }

    @Test
    void resolveFlagOrGeneralFallsBackToGeneralWhenSpecificUnset() {
        Region region = this.manager.createRegion(WORLD, "canal", new CuboidShape(0, 0, 0, 30, 30, 30), 10, null);
        this.manager.setFlag(region, Flags.FLUID_FLOW, GroupTarget.ALL, false);

        assertFalse(this.manager.resolveFlagOrGeneral(WORLD, 5, 5, 5, Flags.WATER_FLOW, Flags.FLUID_FLOW, null),
                "no water-flow set → the general fluid-flow deny applies");
    }

    @Test
    void resolveFlagOrGeneralPrefersTheSpecificWhenSet() {
        Region region = this.manager.createRegion(WORLD, "canal", new CuboidShape(0, 0, 0, 30, 30, 30), 10, null);
        this.manager.setFlag(region, Flags.FLUID_FLOW, GroupTarget.ALL, false);
        this.manager.setFlag(region, Flags.WATER_FLOW, GroupTarget.ALL, true);

        assertTrue(this.manager.resolveFlagOrGeneral(WORLD, 5, 5, 5, Flags.WATER_FLOW, Flags.FLUID_FLOW, null),
                "water-flow allow overrides the fluid-flow deny");
        assertFalse(this.manager.resolveFlagOrGeneral(WORLD, 5, 5, 5, Flags.LAVA_FLOW, Flags.FLUID_FLOW, null),
                "lava (no specific) still follows the general fluid-flow deny");
    }

    @Test
    void resolveFlagOrGeneralUsesDefaultWhenNothingIsSet() {
        this.manager.createRegion(WORLD, "plain", new CuboidShape(0, 0, 0, 30, 30, 30), 10, null);
        assertTrue(this.manager.resolveFlagOrGeneral(WORLD, 5, 5, 5, Flags.WATER_FLOW, Flags.FLUID_FLOW, null),
                "both unset → the general flag's default (allow)");
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
