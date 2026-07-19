package fr.maxlego08.zregions.common.locale;

/**
 * Every user-facing text key, with its English MiniMessage default. The root
 * messages.yml (extracted in the language.yml language) overrides these; a
 * missing key falls back to the default so an outdated file never breaks the plugin.
 *
 * <p>Colours are the semantic {@link Palette} tags ({@code <primary>}, {@code <accent>},
 * {@code <success>}, {@code <error>}, {@code <body>}, {@code <muted>}), never vanilla
 * colour names — the theme is a fixed high-contrast hex palette resolved at render time.</p>
 */
public enum Message {

    PREFIX("prefix", "<muted>[<gradient:#38bdf8:#2563eb>zRegions</gradient><muted>]<reset> "),

    PLAYER_ONLY("commands.player-only", "<prefix><error>This command can only be used by a player."),
    NO_PERMISSION("commands.no-permission", "<prefix><error>You don't have permission to do this."),
    UNKNOWN_COMMAND("commands.unknown", "<prefix><error>Unknown sub-command. Use <accent>/rg help</accent>."),
    HELP_HEADER("commands.help.header", "<prefix><primary>Available commands:"),
    HELP_ENTRY("commands.help.entry", "<muted> • <accent>/rg <usage></accent> <muted>— <body><description>"),
    HELP_HOVER("commands.help.hover", "<body>Click to insert this command."),
    COMMANDS_PAGE("commands.page", "<muted>Page <body><page></body><muted>/<body><pages></body>"),

    POS1_SET("selection.pos1", "<prefix><body>Position <accent>1</accent> set to <accent><position></accent>."),
    POS2_SET("selection.pos2", "<prefix><body>Position <accent>2</accent> set to <accent><position></accent>."),
    SELECTION_INCOMPLETE("selection.incomplete", "<prefix><error>Select two positions first (<accent>/rg pos1</accent> and <accent>/rg pos2</accent>)."),
    SELECTION_WORLD_MISMATCH("selection.world-mismatch", "<prefix><error>The whole selection must be in the same world."),
    SELECTION_RADIUS_TOO_SMALL("selection.radius-too-small", "<prefix><error>Positions 1 and 2 are too close — the radius must be at least <accent>1</accent> block."),
    SELECTION_POINTS_NEEDED("selection.points-needed", "<prefix><error>A polygon needs at least <accent>3</accent> points (<accent>/rg addpoint</accent> or <accent>/rg star</accent>)."),
    POINT_ADDED("selection.point-added", "<prefix><body>Point <accent>#<count></accent> added at <accent><position></accent>."),
    POINTS_CLEARED("selection.points-cleared", "<prefix><body>Polygon points cleared."),
    STAR_GENERATED("selection.star-generated", "<prefix><success>Star with <accent><branches></accent> branches generated (<accent><points></accent> points) around you. Set the height with <accent>/rg pos1</accent>/<accent>pos2</accent>, then create with the <accent>polygon</accent> shape."),
    WAND_NAME("selection.wand.name", "<accent>zRegions wand"),
    WAND_GIVEN("selection.wand.given", "<prefix><success>Wand received: <accent>left click</accent> = position 1, <accent>right click</accent> = position 2."),

    REGION_CREATED("region.created", "<prefix><success>Region <accent><region></accent> created (<body><shape></body>, priority <body><priority></body>)."),
    REGION_ALREADY_EXISTS("region.already-exists", "<prefix><error>A region named <accent><region></accent> already exists in this world."),
    REGION_NAME_RESERVED("region.name-reserved", "<prefix><error>The name <accent><region></accent> is reserved. Use <accent>/rg global</accent> to create the global region."),
    REGION_GLOBAL_CREATED("region.global-created", "<prefix><success>Global region <accent><region></accent> created for world <accent><world></accent> — its flags now apply wherever no region overrides them."),
    REGION_GLOBAL_EXISTS("region.global-exists", "<prefix><body>World <accent><world></accent> already has its global region (<accent><region></accent>)."),
    REGION_NOT_FOUND("region.not-found", "<prefix><error>No region named <accent><region></accent> was found."),
    REGION_AMBIGUOUS("region.ambiguous", "<prefix><error>Several regions are named <accent><region></accent> (worlds: <accent><worlds></accent>). Use <accent>world:<region></accent>."),
    REGION_REMOVED("region.removed", "<prefix><success>Region <accent><region></accent> removed."),
    REGION_REMOVE_CONFIRM("region.remove-confirm", "<prefix><body>Delete region <accent><region></accent>? <error>This cannot be undone.</error> "),
    REGION_REMOVE_CONFIRM_BUTTON("region.remove-confirm-button", "<success>[✔ Confirm]"),
    REGION_REMOVE_CONFIRM_HOVER("region.remove-confirm-hover", "<body>Click to permanently delete this region."),
    REGION_REMOVE_CONFIRM_HINT("region.remove-confirm-hint", "<muted> — or ignore this message."),
    REGION_TELEPORTED("region.teleported", "<prefix><success>Teleported to <accent><region></accent>."),
    REGION_TELEPORT_GLOBAL("region.teleport-global", "<prefix><error>The global region covers the whole world — there is nowhere specific to teleport to."),
    REGION_TELEPORT_NO_SAFE("region.teleport-no-safe", "<prefix><error>No safe spot found to teleport into <accent><region></accent>."),

    REGION_LIST_HEADER("region.list.header", "<prefix><primary>Regions in <accent><world></accent> <muted>(<count>)<primary>:"),
    REGION_LIST_ENTRY("region.list.entry", "<muted> • <accent><region></accent> <muted>(<body><shape></body>, priority <body><priority></body><muted>)"),
    REGION_LIST_HOVER("region.list.hover", "<body>Click for the region details."),
    REGION_LIST_EMPTY("region.list.empty", "<prefix><body>No region in this world yet."),

    REGION_INFO_HEADER("region.info.header", "<prefix><primary>Region <accent><region></accent><primary>:"),
    REGION_INFO_WORLD("region.info.world", "<body> World: <accent><world></accent>"),
    REGION_INFO_SHAPE("region.info.shape", "<body> Shape: <accent><shape></accent>"),
    REGION_INFO_PRIORITY("region.info.priority", "<body> Priority: <accent><priority></accent>"),
    REGION_INFO_MEMBERS("region.info.members", "<body> Members: <accent><members></accent>"),
    REGION_INFO_FLAGS("region.info.flags", "<body> Flags: <accent><flags></accent>"),

    REGION_PRIORITY_SET("region.priority-set", "<prefix><success>Priority of <accent><region></accent> set to <accent><priority></accent>."),
    REGION_PARENT_SET("region.parent-set", "<prefix><success>Region <accent><region></accent> now inherits from <accent><parent></accent>."),
    REGION_PARENT_CLEARED("region.parent-cleared", "<prefix><success>Region <accent><region></accent> no longer has a parent."),
    REGION_PARENT_CYCLE("region.parent-cycle", "<prefix><error>Impossible: this would create an inheritance cycle."),
    REGION_REDEFINED("region.redefined", "<prefix><success>Region <accent><region></accent> redefined from your selection."),
    REGION_REDEFINE_GLOBAL("region.redefine-global", "<prefix><error>The global region has no shape to redefine."),
    REGION_REDEFINE_WORLD_MISMATCH("region.redefine-world-mismatch", "<prefix><error>Your selection is in <accent><world></accent> but the region lives in <accent><region_world></accent>."),

    MEMBER_ADDED("member.added", "<prefix><success><player> is now <accent><role></accent> of <accent><region></accent>."),
    MEMBER_REMOVED("member.removed", "<prefix><success><player> removed from <accent><region></accent>."),
    MEMBER_NOT_MEMBER("member.not-member", "<prefix><error><player> is not a member of <accent><region></accent>."),
    MEMBER_INVALID_ROLE("member.invalid-role", "<prefix><error>Unknown role <accent><role></accent>. Use <accent>owner</accent> or <accent>member</accent>."),
    PLAYER_NOT_FOUND("commands.player-not-found", "<prefix><error>Unknown player <accent><player></accent>."),

    FLAG_SET("flag.set", "<prefix><success>Flag <accent><flag></accent> set to <accent><value></accent> for <accent><target></accent> in <accent><region></accent>."),
    FLAG_UNSET("flag.unset", "<prefix><success>Flag <accent><flag></accent> removed for <accent><target></accent> in <accent><region></accent>."),
    FLAG_UNKNOWN("flag.unknown", "<prefix><error>Unknown flag <accent><flag></accent>."),
    FLAG_VALUE_INVALID("flag.invalid-value", "<prefix><error>Invalid value <accent><value></accent> for flag <accent><flag></accent>."),
    FLAG_TARGET_INVALID("flag.invalid-target", "<prefix><error>Unknown target <accent><target></accent>. Use <accent>owner</accent>, <accent>member</accent>, <accent>visitor</accent> or <accent>all</accent>."),

    FLAG_LIST_HEADER("flag.list.header", "<prefix><primary>Flag catalogue <muted>(<count>)<primary>:"),
    FLAG_LIST_ENTRY("flag.list.entry", "<muted> • <accent><flag></accent> <muted>— <body><type>"),
    FLAG_LIST_HOVER("flag.list.hover", "<accent><flag></accent><newline><body><description><newline><muted>Click to start a /rg flag command."),

    GUI_UNAVAILABLE("gui.unavailable", "<prefix><error>The GUI requires the <accent>zMenu</accent> plugin. Everything stays available through commands — see <accent>/rg help</accent>."),
    GUI_REGION_GONE("gui.region-gone", "<prefix><error>This region no longer exists."),
    GUI_FLAG_COMMAND_ONLY("gui.flag-command-only", "<prefix><body>Flag <accent><flag></accent> holds a value — set it with <accent>/rg flag <region> <flag> …</accent>."),

    BORDER_SHOWN("border.shown", "<prefix><success>Showing the borders of <accent><region></accent> for <accent><seconds></accent>s (only you can see them)."),
    BORDER_GLOBAL("border.global", "<prefix><error>The global region covers the whole world — there is nothing to outline."),
    BORDER_OTHER_WORLD("border.other-world", "<prefix><error>Region <accent><region></accent> is in <accent><region_world></accent>, you are in <accent><world></accent>."),

    ACTION_DENIED("protection.denied", "<prefix><error>You can't do that here."),
    ENTRY_DENIED("protection.entry-denied", "<prefix><error>You can't enter <accent><region></accent>."),
    EXIT_DENIED("protection.exit-denied", "<prefix><error>You can't leave <accent><region></accent>."),

    IMPORT_UNKNOWN_SOURCE("import.unknown-source", "<prefix><error>Unknown import source <accent><source></accent>. Available: <accent><sources></accent>."),
    IMPORT_SOURCE_NOT_FOUND("import.source-not-found", "<prefix><error>No <source> data found (<accent><path></accent>)."),
    IMPORT_STARTED("import.started", "<prefix><body>Importing <accent><source></accent> regions…"),
    IMPORT_DONE("import.done", "<prefix><success>Import finished: <accent><imported></accent> region(s) imported, <accent><skipped></accent> skipped, <accent><flags></accent> flag value(s), <accent><members></accent> member(s). Details in the server console."),
    IMPORT_DRY_RUN("import.dry-run", "<prefix><body>Dry-run only — nothing was written. Run without <accent>--dry-run</accent> to apply."),

    RELOADED("commands.reloaded", "<prefix><success>Configuration and messages reloaded.");

    private final String path;
    private final String def;

    Message(String path, String def) {
        this.path = path;
        this.def = def;
    }

    public String getPath() {
        return this.path;
    }

    public String getDefault() {
        return this.def;
    }
}
