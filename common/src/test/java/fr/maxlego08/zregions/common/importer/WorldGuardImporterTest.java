package fr.maxlego08.zregions.common.importer;

import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.ShapeType;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.region.TestPluginFixture;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Feeds the WorldGuard importer with realistic {@code regions.yml} content
 * (parsed by the same SafeConstructor Yaml as production) and asserts the
 * resulting regions, shapes, flags, members, parents and report counters.
 */
class WorldGuardImporterTest {

    private static final String WORLD = "world";
    private static final UUID OWNER = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

    private TestPluginFixture plugin;
    private ZRegionManager manager;
    private WorldGuardImporter importer;

    @BeforeEach
    void setUp() {
        this.plugin = new TestPluginFixture();
        this.manager = new ZRegionManager(this.plugin);
        this.plugin.setRegionManager(this.manager);
        this.importer = new WorldGuardImporter(this.plugin);
    }

    private ImportReport importYaml(String yaml, boolean dryRun) {
        Map<String, Object> root = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        ImportReport report = new ImportReport(dryRun);
        this.importer.importWorld(WORLD, root, report);
        return report;
    }

    @Test
    void cuboidImportsWithPriorityFlagsAndOwner() {
        ImportReport report = importYaml("""
                regions:
                  spawn:
                    type: cuboid
                    min: {x: -10.0, y: 0.0, z: -10.0}
                    max: {x: 10.0, y: 128.0, z: 10.0}
                    priority: 20
                    flags: {pvp: deny, greeting: 'Welcome!'}
                    owners: {unique-ids: [%s]}
                    members: {}
                """.formatted(OWNER), false);

        Region region = this.manager.getRegion(WORLD, "spawn").orElseThrow();
        assertEquals(20, region.getPriority());
        assertEquals(ShapeType.CUBOID, region.getShape().getType());
        assertTrue(region.getShape().contains(10.5, 64, 10.5), "the max block is inclusive, like WorldGuard");
        assertFalse(this.manager.resolveFlag(region, Flags.PVP, null));
        assertEquals("Welcome!", this.manager.resolveFlag(region, Flags.GREETING, null));
        assertTrue(region.hasRole(OWNER, MemberRole.OWNER));
        assertEquals(1, report.getRegionsImported());
        assertEquals(2, report.getFlagsApplied());
        assertEquals(1, report.getMembersImported());
    }

    @Test
    void poly2dBecomesAPolygonPrism() {
        importYaml("""
                regions:
                  star:
                    type: poly2d
                    min-y: 10
                    max-y: 60
                    points: [{x: 0, z: 0}, {x: 20, z: 0}, {x: 10, z: 20}]
                    flags: {}
                """, false);

        Region region = this.manager.getRegion(WORLD, "star").orElseThrow();
        assertEquals(ShapeType.POLYGON, region.getShape().getType());
        assertTrue(region.getShape().contains(10, 30, 5), "inside the triangle and the Y range");
        assertFalse(region.getShape().contains(10, 70, 5), "above max-y");
    }

    @Test
    void wgGlobalRegionBecomesTheWorldGlobal() {
        ImportReport report = importYaml("""
                regions:
                  __global__:
                    type: global
                    flags: {pvp: deny}
                    members: {unique-ids: [%s]}
                """.formatted(OWNER), false);

        Region global = this.manager.getGlobalRegion(WORLD).orElseThrow();
        assertTrue(global.isGlobal());
        assertFalse(this.manager.resolveFlag(WORLD, 999, 64, 999, Flags.PVP, null),
                "the imported global deny applies world-wide");
        assertTrue(global.hasRole(OWNER, MemberRole.MEMBER), "global-region members survive the import");
        assertEquals(1, report.getRegionsImported());
        assertEquals(1, report.getMembersImported());
        assertTrue(this.manager.resolveFlag(WORLD, 0, 64, 0, Flags.BLOCK_BREAK, UUID.randomUUID()),
                "the global region gets no implicit membership protection");
    }

    @Test
    void poly2dBoundaryBlockColumnsStayProtectedLikeWorldGuard() {
        // WG protects the block columns ON the outline; the importer widens the polygon
        importYaml("""
                regions:
                  plot:
                    type: poly2d
                    min-y: 0
                    max-y: 100
                    points: [{x: 0, z: 0}, {x: 10, z: 0}, {x: 10, z: 10}, {x: 0, z: 10}]
                    flags: {}
                """, false);

        Region region = this.manager.getRegion(WORLD, "plot").orElseThrow();
        assertTrue(region.getShape().contains(10.5, 50, 5.5), "the x=10 boundary column is protected in WG");
        assertTrue(region.getShape().contains(5.5, 50, 10.5), "the z=10 boundary column is protected in WG");
        assertTrue(region.getShape().contains(10.5, 50, 10.5), "the max corner block is protected in WG");
        assertFalse(region.getShape().contains(11.5, 50, 5.5), "one block past the boundary stays outside");
    }

    @Test
    void existingGlobalRegionIsNotOverwritten() {
        this.manager.createGlobalRegion(WORLD);

        ImportReport report = importYaml("""
                regions:
                  __global__:
                    type: global
                    flags: {pvp: deny}
                """, false);

        assertEquals(0, report.getRegionsImported());
        assertEquals(1, report.getRegionsSkipped());
        assertTrue(this.manager.resolveFlag(WORLD, 0, 64, 0, Flags.PVP, null),
                "the pre-existing global region keeps its (default) flags");
    }

    @Test
    void parentLinksResolveWhateverTheFileOrder() {
        importYaml("""
                regions:
                  child:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    parent: base
                    flags: {}
                  base:
                    type: cuboid
                    min: {x: 100, y: 0, z: 100}
                    max: {x: 120, y: 50, z: 120}
                    flags: {pvp: deny}
                """, false);

        Region child = this.manager.getRegion(WORLD, "child").orElseThrow();
        Region base = this.manager.getRegion(WORLD, "base").orElseThrow();
        assertEquals(base.getId(), child.getParentId().orElseThrow(), "child precedes its parent in the file");
        assertFalse(this.manager.resolveFlag(child, Flags.PVP, null), "flags inherited through the imported link");
    }

    @Test
    void missingParentIsReportedAndDropped() {
        ImportReport report = importYaml("""
                regions:
                  orphan:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    parent: ghost
                    flags: {}
                """, false);

        Region orphan = this.manager.getRegion(WORLD, "orphan").orElseThrow();
        assertTrue(orphan.getParentId().isEmpty());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("ghost")),
                "the dropped link must be reported");
    }

    @Test
    void blockedCmdsBecomesTheCommandBlacklist() {
        importYaml("""
                regions:
                  jail:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {blocked-cmds: ['/tp', '/home', '/back']}
                """, false);

        Region region = this.manager.getRegion(WORLD, "jail").orElseThrow();
        assertEquals(List.of("/tp", "/home", "/back"),
                this.manager.resolveFlag(region, Flags.COMMAND_BLACKLIST, null));
    }

    @Test
    void buildExpandsToBreakPlaceInteractAndContainers() {
        ImportReport report = importYaml("""
                regions:
                  protected:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {build: deny}
                """, false);

        Region region = this.manager.getRegion(WORLD, "protected").orElseThrow();
        assertFalse(this.manager.resolveFlag(region, Flags.BLOCK_BREAK, null));
        assertFalse(this.manager.resolveFlag(region, Flags.BLOCK_PLACE, null));
        assertFalse(this.manager.resolveFlag(region, Flags.INTERACT, null));
        assertFalse(this.manager.resolveFlag(region, Flags.CONTAINER_ACCESS, null));
        assertEquals(4, report.getFlagsApplied());
    }

    @Test
    void entryAndExitImportAtTheVisitorTarget() {
        // WG declares entry/exit with a NON_MEMBERS default group: members must keep access
        importYaml("""
                regions:
                  staff:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {entry: deny, exit: deny}
                    members: {unique-ids: [%s]}
                """.formatted(OWNER), false);

        Region region = this.manager.getRegion(WORLD, "staff").orElseThrow();
        assertTrue(this.manager.resolveFlag(region, Flags.ENTRY, OWNER), "a member enters their own region");
        assertTrue(this.manager.resolveFlag(region, Flags.EXIT, OWNER), "a member is never trapped inside");
        UUID visitor = UUID.randomUUID();
        assertFalse(this.manager.resolveFlag(region, Flags.ENTRY, visitor), "visitors stay locked out, like WG");
        assertFalse(this.manager.resolveFlag(region, Flags.EXIT, visitor));
    }

    @Test
    void implicitMembershipProtectionIsReproduced() {
        // the typical WG claim: owners, empty flags — WG denies building to non-members by default
        importYaml("""
                regions:
                  home:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {}
                    owners: {unique-ids: [%s]}
                """.formatted(OWNER), false);

        Region region = this.manager.getRegion(WORLD, "home").orElseThrow();
        UUID visitor = UUID.randomUUID();
        assertFalse(this.manager.resolveFlag(region, Flags.BLOCK_BREAK, visitor));
        assertFalse(this.manager.resolveFlag(region, Flags.BLOCK_PLACE, visitor));
        assertFalse(this.manager.resolveFlag(region, Flags.INTERACT, visitor));
        assertFalse(this.manager.resolveFlag(region, Flags.CONTAINER_ACCESS, visitor));
        assertTrue(this.manager.resolveFlag(region, Flags.BLOCK_BREAK, OWNER), "the owner still builds");
    }

    @Test
    void passthroughAllowDisablesTheImplicitProtection() {
        importYaml("""
                regions:
                  overlay:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {passthrough: allow}
                """, false);

        Region region = this.manager.getRegion(WORLD, "overlay").orElseThrow();
        assertTrue(this.manager.resolveFlag(region, Flags.BLOCK_BREAK, UUID.randomUUID()),
                "a passthrough overlay must not protect anything");
    }

    @Test
    void explicitWgFlagsSuppressTheMatchingImplicitDeny() {
        importYaml("""
                regions:
                  open:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {block-break: allow}
                """, false);

        Region region = this.manager.getRegion(WORLD, "open").orElseThrow();
        UUID visitor = UUID.randomUUID();
        assertTrue(this.manager.resolveFlag(region, Flags.BLOCK_BREAK, visitor),
                "an explicit WG allow overrides the membership default, like in WG");
        assertFalse(this.manager.resolveFlag(region, Flags.BLOCK_PLACE, visitor),
                "the other implicit denies still apply");
    }

    @Test
    void conflictingExplosionFlagsResolveToDeny() {
        importYaml("""
                regions:
                  base:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {creeper-explosion: allow, other-explosion: deny}
                """, false);

        Region region = this.manager.getRegion(WORLD, "base").orElseThrow();
        assertFalse(this.manager.resolveFlag(region, Flags.ENTITY_EXPLOSION, null),
                "when WG flags feeding one zRegions flag conflict, deny must win");
    }

    @Test
    void unmappedAndGroupFlagsAreSkippedWithDetails() {
        ImportReport report = importYaml("""
                regions:
                  base:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {heal-amount: 5, pvp-group: members, pvp: deny}
                """, false);

        assertEquals(1, report.getFlagsApplied());
        assertEquals(2, report.getFlagsSkipped());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("heal-amount")));
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("pvp-group")));
    }

    @Test
    void nameCollisionIsSkippedAndReported() {
        this.manager.createRegion(WORLD, "spawn", new CuboidShape(50, 0, 50, 60, 10, 60), 5, null);

        ImportReport report = importYaml("""
                regions:
                  spawn:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {pvp: deny}
                """, false);

        assertEquals(0, report.getRegionsImported());
        assertEquals(1, report.getRegionsSkipped());
        Region existing = this.manager.getRegion(WORLD, "spawn").orElseThrow();
        assertTrue(this.manager.resolveFlag(existing, Flags.PVP, null), "the existing region is untouched");
    }

    @Test
    void nameBasedAndGroupMembersAreSkippedWithDetails() {
        ImportReport report = importYaml("""
                regions:
                  base:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {}
                    owners: {unique-ids: [%s], players: [Notch]}
                    members: {groups: [vip]}
                """.formatted(OWNER), false);

        assertEquals(1, report.getMembersImported());
        assertEquals(2, report.getMembersSkipped());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("Notch")));
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("vip")));
    }

    @Test
    void greetingTitleMapsToTheTitleFlag() {
        importYaml("""
                regions:
                  base:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {greeting-title: 'Welcome'}
                """, false);

        Region region = this.manager.getRegion(WORLD, "base").orElseThrow();
        assertEquals("Welcome", this.manager.resolveFlag(region, Flags.TITLE, null));
    }

    @Test
    void unsupportedShapeIsSkippedAndReported() {
        ImportReport report = importYaml("""
                regions:
                  weird:
                    type: hexagon
                    flags: {}
                """, false);

        assertEquals(0, report.getRegionsImported());
        assertEquals(1, report.getRegionsSkipped());
        assertTrue(this.manager.getRegions().isEmpty());
    }

    @Test
    void dryRunCountsEverythingButWritesNothing() {
        ImportReport report = importYaml("""
                regions:
                  __global__:
                    type: global
                    flags: {pvp: deny}
                  spawn:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    priority: 10
                    flags: {pvp: deny, heal-amount: 5}
                    owners: {unique-ids: [%s]}
                    parent: ghost
                """.formatted(OWNER), true);

        assertTrue(this.manager.getRegions().isEmpty(), "dry-run must not create anything");
        assertTrue(this.manager.getGlobalRegion(WORLD).isEmpty());
        assertEquals(2, report.getRegionsImported(), "the report still counts what WOULD be imported");
        assertEquals(2, report.getFlagsApplied());
        assertEquals(1, report.getFlagsSkipped());
        assertEquals(1, report.getMembersImported());
        assertTrue(report.getDetails().stream().anyMatch(line -> line.contains("ghost")),
                "a missing parent is validated even in dry-run");
    }

    @Test
    void reimportingAnAlreadyImportedNameSkipsIt() {
        // YAML maps cannot hold duplicate keys, but a second world file could redeclare
        // a name imported by a previous call: the second import must skip it.
        importYaml("""
                regions:
                  spawn:
                    type: cuboid
                    min: {x: 0, y: 0, z: 0}
                    max: {x: 5, y: 5, z: 5}
                    flags: {}
                """, false);
        ImportReport second = importYaml("""
                regions:
                  spawn:
                    type: cuboid
                    min: {x: 10, y: 0, z: 10}
                    max: {x: 15, y: 5, z: 15}
                    flags: {}
                """, false);

        assertEquals(0, second.getRegionsImported());
        assertEquals(1, second.getRegionsSkipped());
    }
}
