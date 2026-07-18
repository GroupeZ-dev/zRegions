package fr.maxlego08.zregions.common.importer;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.RegionShape;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import fr.maxlego08.zregions.common.shape.PolygonShape;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static java.util.Map.entry;

/**
 * Imports WorldGuard regions by reading its {@code worlds/<world>/regions.yml}
 * files directly — no WorldGuard API, so the import works even after WorldGuard
 * was removed, and the whole logic stays platform-agnostic and testable.
 *
 * <p>Mapped: cuboid and poly2d shapes (WG cylinders don't exist), the
 * {@code __global__} region, priority, parent links (second pass), UUID-based
 * owners/members, and every WG flag with a zRegions equivalent — including
 * {@code blocked-cmds} → {@code command-blacklist} and {@code build}, which
 * expands to block-break/block-place/interact. When several WG flags feed one
 * zRegions flag (the explosion family), deny wins. Everything unmapped lands in
 * the report's detail lines — never dropped silently (plan §15).</p>
 */
public final class WorldGuardImporter {

    public static final String SOURCE_NAME = "worldguard";
    private static final String REGIONS_FILE = "regions.yml";

    /** WG state flag → zRegions state flag (same allow/deny semantics). */
    private static final Map<String, Flag<Boolean>> STATE_FLAGS = Map.ofEntries(
            entry("pvp", Flags.PVP),
            entry("block-break", Flags.BLOCK_BREAK),
            entry("block-place", Flags.BLOCK_PLACE),
            entry("chest-access", Flags.CONTAINER_ACCESS),
            entry("use", Flags.INTERACT),
            entry("interact", Flags.INTERACT),
            entry("damage-animals", Flags.DAMAGE_ANIMALS),
            entry("mob-spawning", Flags.MOB_SPAWNING),
            entry("mob-damage", Flags.MOB_DAMAGE),
            entry("enderman-grief", Flags.MOB_GRIEFING),
            entry("creeper-explosion", Flags.ENTITY_EXPLOSION),
            entry("ghast-fireball", Flags.ENTITY_EXPLOSION),
            entry("tnt", Flags.ENTITY_EXPLOSION),
            entry("pistons", Flags.PISTON),
            entry("entity-painting-destroy", Flags.HANGING_BREAK),
            entry("entity-item-frame-destroy", Flags.HANGING_BREAK),
            entry("fire-spread", Flags.FIRE_SPREAD),
            entry("lighter", Flags.FIRE_IGNITE),
            entry("water-flow", Flags.FLUID_FLOW),
            entry("lava-flow", Flags.FLUID_FLOW),
            entry("leaf-decay", Flags.LEAF_DECAY),
            entry("block-trampling", Flags.CROP_TRAMPLE),
            entry("vehicle-place", Flags.VEHICLE_PLACE),
            entry("vehicle-destroy", Flags.VEHICLE_DESTROY),
            entry("entry", Flags.ENTRY),
            entry("exit", Flags.EXIT),
            entry("enderpearl", Flags.ENDERPEARL),
            entry("chorus-fruit-teleport", Flags.CHORUS_FRUIT),
            entry("exp-drops", Flags.EXP_DROP),
            entry("item-drop", Flags.ITEM_DROP),
            entry("item-pickup", Flags.ITEM_PICKUP),
            entry("fall-damage", Flags.FALL_DAMAGE),
            entry("invincible", Flags.INVINCIBLE),
            entry("send-chat", Flags.CHAT));

    /** WG text flag → zRegions text flag (values imported verbatim). */
    private static final Map<String, Flag<String>> TEXT_FLAGS = Map.of(
            "greeting", Flags.GREETING,
            "farewell", Flags.FAREWELL,
            "greeting-title", Flags.TITLE);

    private final ZRegionsPlugin plugin;

    public WorldGuardImporter(ZRegionsPlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Imports every {@code <world>/regions.yml} found under WorldGuard's
     * {@code worlds/} directory. A corrupted file is reported and skipped, it
     * never aborts the other worlds.
     */
    public ImportReport importAll(Path worldsDirectory, boolean dryRun) {
        ImportReport report = new ImportReport(dryRun);
        // SafeConstructor: data-only YAML, no arbitrary type construction. The default
        // 3 MiB code-point limit is too small for large servers' regions.yml — raised.
        LoaderOptions options = new LoaderOptions();
        options.setCodePointLimit(64 * 1024 * 1024);
        Yaml yaml = new Yaml(new SafeConstructor(options));

        try (DirectoryStream<Path> worlds = Files.newDirectoryStream(worldsDirectory, Files::isDirectory)) {
            for (Path worldDirectory : worlds) {
                Path file = worldDirectory.resolve(REGIONS_FILE);
                if (!Files.isRegularFile(file)) {
                    continue;
                }
                String worldName = worldDirectory.getFileName().toString();
                try (Reader reader = Files.newBufferedReader(file)) {
                    Object root = yaml.load(reader);
                    importWorld(worldName, asMap(root), report);
                } catch (Exception exception) {
                    report.error("world '" + worldName + "': unreadable " + REGIONS_FILE + " — " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            report.error("unable to list " + worldsDirectory + " — " + exception.getMessage());
        }
        return report;
    }

    /**
     * Imports the parsed content of one world's {@code regions.yml} (the map
     * holding the top-level {@code regions:} key). Public for tests — the file
     * walking above is the only platform-independent I/O around it.
     */
    public void importWorld(String worldName, Map<String, Object> root, ImportReport report) {
        Map<String, Object> regions = asMap(root == null ? null : root.get("regions"));
        RegionManager manager = this.plugin.getRegionManager();

        // first pass: create the regions; parents link once every name exists
        Map<String, Region> imported = new HashMap<>();
        Set<String> planned = new HashSet<>();
        Map<String, String> parents = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : regions.entrySet()) {
            String name = entry.getKey();
            Map<String, Object> data = asMap(entry.getValue());
            String context = "world '" + worldName + "': region '" + name + "'";
            String type = String.valueOf(data.getOrDefault("type", "cuboid")).toLowerCase(Locale.ROOT);

            try {
                Region region;
                if (type.equals("global")) {
                    if (manager.getGlobalRegion(worldName).isPresent()
                            || planned.contains(RegionManager.GLOBAL_REGION_NAME)) {
                        report.regionSkipped(context + " skipped — the world already has a global region");
                        continue;
                    }
                    region = report.isDryRun() ? null : manager.createGlobalRegion(worldName);
                    planned.add(RegionManager.GLOBAL_REGION_NAME);
                } else {
                    String lower = name.toLowerCase(Locale.ROOT);
                    if (RegionManager.GLOBAL_REGION_NAME.equalsIgnoreCase(name)) {
                        report.regionSkipped(context + " skipped — the name is reserved for the global region");
                        continue;
                    }
                    if (manager.getRegion(worldName, name).isPresent() || planned.contains(lower)) {
                        report.regionSkipped(context + " skipped — a region with this name already exists");
                        continue;
                    }
                    RegionShape shape = parseShape(type, data);
                    int priority = asInt(data.get("priority"), 0);
                    region = report.isDryRun() ? null
                            : manager.createRegion(worldName, name, shape, priority, null);
                    planned.add(lower);
                }

                Map<String, Object> flags = asMap(data.get("flags"));
                applyFlags(region, flags, context, report);
                if (!type.equals("global")) {
                    applyImplicitProtection(region, flags);
                }
                applyMembers(region, data.get("owners"), MemberRole.OWNER, context, report);
                applyMembers(region, data.get("members"), MemberRole.MEMBER, context, report);
                Object parent = data.get("parent");
                if (parent != null) {
                    parents.put(name, String.valueOf(parent));
                }
                if (region != null) {
                    imported.put(name.toLowerCase(Locale.ROOT), region);
                }
                report.regionImported();
            } catch (IllegalArgumentException exception) {
                report.regionSkipped(context + " skipped — " + exception.getMessage());
            }
        }

        // second pass: parent links (a child may precede its parent in the file)
        for (Map.Entry<String, String> entry : parents.entrySet()) {
            String childName = entry.getKey();
            String parentName = entry.getValue();
            String context = "world '" + worldName + "': region '" + childName + "'";

            if (report.isDryRun()) {
                if (!planned.contains(parentName.toLowerCase(Locale.ROOT))
                        && manager.getRegion(worldName, parentName).isEmpty()) {
                    report.error(context + ": parent '" + parentName + "' not found — link would be dropped");
                }
                continue;
            }
            Region child = imported.get(childName.toLowerCase(Locale.ROOT));
            Region parent = imported.get(parentName.toLowerCase(Locale.ROOT));
            if (parent == null) {
                parent = manager.getRegion(worldName, parentName).orElse(null);
            }
            if (child == null) {
                continue;
            }
            if (parent == null) {
                report.error(context + ": parent '" + parentName + "' not found — link dropped");
                continue;
            }
            try {
                manager.setParent(child, parent);
            } catch (IllegalArgumentException exception) {
                report.error(context + ": parent '" + parentName + "' rejected — " + exception.getMessage());
            }
        }
    }

    // --- shapes ---

    private static RegionShape parseShape(String type, Map<String, Object> data) {
        return switch (type) {
            case "cuboid" -> {
                Map<String, Object> min = asMap(data.get("min"));
                Map<String, Object> max = asMap(data.get("max"));
                if (min.isEmpty() || max.isEmpty()) {
                    throw new IllegalArgumentException("cuboid without min/max corners");
                }
                yield new CuboidShape(
                        blockCoordinate(min.get("x")), blockCoordinate(min.get("y")), blockCoordinate(min.get("z")),
                        blockCoordinate(max.get("x")), blockCoordinate(max.get("y")), blockCoordinate(max.get("z")));
            }
            case "poly2d" -> {
                List<Object> rawPoints = asList(data.get("points"));
                List<double[]> points = new ArrayList<>(rawPoints.size());
                for (Object rawPoint : rawPoints) {
                    Map<String, Object> point = asMap(rawPoint);
                    points.add(new double[]{asDouble(point.get("x")), asDouble(point.get("z"))});
                }
                yield new PolygonShape(expandForBlockInclusion(points),
                        asInt(data.get("min-y"), 0), asInt(data.get("max-y"), 255));
            }
            default -> throw new IllegalArgumentException("unsupported shape type '" + type + "'");
        };
    }

    /**
     * WorldGuard protects the block columns lying ON the poly2d outline (edge and
     * vertex blocks included), while a raw even-odd polygon excludes its max-facing
     * boundary. Growing every max-side vertex by one block keeps those columns
     * protected: exact for axis-aligned outlines, slightly over-inclusive on
     * concave ones — always safer than leaving a grief strip along the border.
     */
    private static List<double[]> expandForBlockInclusion(List<double[]> points) {
        double centroidX = 0, centroidZ = 0;
        for (double[] point : points) {
            centroidX += point[0];
            centroidZ += point[1];
        }
        centroidX /= points.size();
        centroidZ /= points.size();

        List<double[]> expanded = new ArrayList<>(points.size());
        for (double[] point : points) {
            expanded.add(new double[]{
                    point[0] + (point[0] >= centroidX ? 1 : 0),
                    point[1] + (point[1] >= centroidZ ? 1 : 0)});
        }
        return expanded;
    }

    // --- flags ---

    private void applyFlags(Region region, Map<String, Object> flags, String context, ImportReport report) {
        // several WG flags can feed one zRegions flag (the explosion family): deny wins
        Map<Flag<Boolean>, Boolean> states = new LinkedHashMap<>();
        Map<Flag<String>, String> texts = new LinkedHashMap<>();
        List<String> blockedCommands = List.of();

        for (Map.Entry<String, Object> entry : flags.entrySet()) {
            String key = entry.getKey().toLowerCase(Locale.ROOT);
            Object value = entry.getValue();

            if (key.endsWith("-group")) {
                report.flagSkipped(context + ": flag '" + key + "' skipped — group-targeted values are not imported");
                continue;
            }
            if (key.equals("passthrough")) {
                // consumed by applyImplicitProtection (allow = non-protecting overlay)
                continue;
            }
            if (key.equals("blocked-cmds")) {
                blockedCommands = asList(value).stream().map(String::valueOf).map(String::trim)
                        .filter(command -> !command.isEmpty()).toList();
                if (blockedCommands.isEmpty()) {
                    report.flagSkipped(context + ": flag 'blocked-cmds' skipped — empty command list");
                }
                continue;
            }
            if (key.equals("build")) {
                // WG's build umbrella covers container use too
                mergeState(states, Flags.BLOCK_BREAK, value, context, key, report);
                mergeState(states, Flags.BLOCK_PLACE, value, context, key, report);
                mergeState(states, Flags.INTERACT, value, context, key, report);
                mergeState(states, Flags.CONTAINER_ACCESS, value, context, key, report);
                continue;
            }
            if (key.equals("other-explosion")) {
                // covers bed/anchor blasts too, which Bukkit reports as block explosions
                mergeState(states, Flags.ENTITY_EXPLOSION, value, context, key, report);
                mergeState(states, Flags.BLOCK_EXPLOSION, value, context, key, report);
                continue;
            }
            Flag<Boolean> stateFlag = STATE_FLAGS.get(key);
            if (stateFlag != null) {
                mergeState(states, stateFlag, value, context, key, report);
                continue;
            }
            Flag<String> textFlag = TEXT_FLAGS.get(key);
            if (textFlag != null) {
                texts.put(textFlag, String.valueOf(value));
                continue;
            }
            report.flagSkipped(context + ": flag '" + key + "' skipped — no zRegions equivalent");
        }

        RegionManager manager = this.plugin.getRegionManager();
        states.forEach((flag, value) -> {
            // WG declares entry/exit with a NON_MEMBERS default group: 'entry: deny'
            // never locks members/owners out — the visitor target reproduces that.
            GroupTarget target = flag == Flags.ENTRY || flag == Flags.EXIT
                    ? GroupTarget.VISITOR : GroupTarget.ALL;
            if (region != null) manager.setFlag(region, flag, target, value);
            report.flagApplied();
        });
        texts.forEach((flag, value) -> {
            if (region != null) manager.setFlag(region, flag, GroupTarget.ALL, value);
            report.flagApplied();
        });
        if (!blockedCommands.isEmpty()) {
            if (region != null) manager.setFlag(region, Flags.COMMAND_BLACKLIST, GroupTarget.ALL, blockedCommands);
            report.flagApplied();
        }
    }

    /**
     * WorldGuard's core protection is not a stored flag: every region denies
     * building to non-members by default (unless {@code passthrough: allow} turns
     * it into a plain overlay). zRegions flags default to allow, so that implicit
     * rule must become explicit visitor-targeted denies — otherwise the typical
     * WG claim ({@code owners} + empty {@code flags}) would import unprotected.
     * An explicit WG flag overrides the membership default there, so each deny is
     * only synthesized when none of its WG sources is present. Not counted as
     * applied flags: they don't come from the file (documented behavior, §10).
     */
    private void applyImplicitProtection(Region region, Map<String, Object> wgFlags) {
        if ("allow".equalsIgnoreCase(String.valueOf(wgFlags.get("passthrough")))) {
            return;
        }
        RegionManager manager = this.plugin.getRegionManager();
        boolean build = wgFlags.containsKey("build");
        if (!build && !wgFlags.containsKey("block-break")) {
            if (region != null) manager.setFlag(region, Flags.BLOCK_BREAK, GroupTarget.VISITOR, false);
        }
        if (!build && !wgFlags.containsKey("block-place")) {
            if (region != null) manager.setFlag(region, Flags.BLOCK_PLACE, GroupTarget.VISITOR, false);
        }
        if (!build && !wgFlags.containsKey("use") && !wgFlags.containsKey("interact")) {
            if (region != null) manager.setFlag(region, Flags.INTERACT, GroupTarget.VISITOR, false);
        }
        if (!build && !wgFlags.containsKey("chest-access")) {
            if (region != null) manager.setFlag(region, Flags.CONTAINER_ACCESS, GroupTarget.VISITOR, false);
        }
    }

    private static void mergeState(Map<Flag<Boolean>, Boolean> states, Flag<Boolean> flag, Object value,
                                   String context, String wgKey, ImportReport report) {
        Boolean parsed = flag.parse(String.valueOf(value).trim()).orElse(null);
        if (parsed == null) {
            report.flagSkipped(context + ": flag '" + wgKey + "' skipped — unreadable value '" + value + "'");
            return;
        }
        states.merge(flag, parsed, Boolean::logicalAnd);
    }

    // --- members ---

    private void applyMembers(Region region, Object section, MemberRole role, String context, ImportReport report) {
        Map<String, Object> domain = asMap(section);
        for (Object rawId : asList(domain.get("unique-ids"))) {
            try {
                UUID playerId = UUID.fromString(String.valueOf(rawId));
                if (region != null) {
                    this.plugin.getRegionManager().setMember(region, playerId, role);
                }
                report.memberImported();
            } catch (IllegalArgumentException exception) {
                report.memberSkipped(context + ": " + role.name().toLowerCase(Locale.ROOT)
                        + " '" + rawId + "' skipped — not a UUID");
            }
        }
        for (Object player : asList(domain.get("players"))) {
            report.memberSkipped(context + ": " + role.name().toLowerCase(Locale.ROOT)
                    + " '" + player + "' skipped — name-based entry without UUID");
        }
        for (Object group : asList(domain.get("groups"))) {
            report.memberSkipped(context + ": " + role.name().toLowerCase(Locale.ROOT)
                    + " group '" + group + "' skipped — permission groups are not imported");
        }
    }

    // --- YAML helpers (SafeConstructor yields Maps, Lists and Numbers) ---

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : Map.of();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object value) {
        return value instanceof List ? (List<Object>) value : List.of();
    }

    private static int asInt(Object value, int def) {
        return value instanceof Number number ? number.intValue() : def;
    }

    private static double asDouble(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalArgumentException("expected a number, got '" + value + "'");
        }
        return number.doubleValue();
    }

    private static int blockCoordinate(Object value) {
        return (int) Math.floor(asDouble(value));
    }
}
