package fr.maxlego08.zregions.common.locale;

/**
 * Every user-facing text key, with its English MiniMessage default. The per-language
 * messages_<lang>.yml files override these; a missing key falls back to the default
 * so an outdated language file never breaks the plugin.
 */
public enum Message {

    PREFIX("prefix", "<dark_gray>[<gradient:#38bdf8:#2563eb>zRegions</gradient><dark_gray>]<reset> "),

    PLAYER_ONLY("commands.player-only", "<prefix><red>This command can only be used by a player."),
    NO_PERMISSION("commands.no-permission", "<prefix><red>You don't have permission to do this."),
    UNKNOWN_COMMAND("commands.unknown", "<prefix><red>Unknown sub-command. Use <yellow>/rg help</yellow>."),
    HELP_HEADER("commands.help.header", "<prefix><gray>Available commands:"),
    HELP_ENTRY("commands.help.entry", "<gray> • <yellow>/rg <usage></yellow> <dark_gray>- <gray><description>"),

    POS1_SET("selection.pos1", "<prefix><gray>Position <yellow>1</yellow> set to <yellow><position></yellow>."),
    POS2_SET("selection.pos2", "<prefix><gray>Position <yellow>2</yellow> set to <yellow><position></yellow>."),
    SELECTION_INCOMPLETE("selection.incomplete", "<prefix><red>Select two positions first (<yellow>/rg pos1</yellow> and <yellow>/rg pos2</yellow>)."),
    SELECTION_WORLD_MISMATCH("selection.world-mismatch", "<prefix><red>Both positions must be in the same world."),

    REGION_CREATED("region.created", "<prefix><green>Region <yellow><region></yellow> created (<gray><shape></gray>, priority <gray><priority></gray>)."),
    REGION_ALREADY_EXISTS("region.already-exists", "<prefix><red>A region named <yellow><region></yellow> already exists in this world."),
    REGION_NOT_FOUND("region.not-found", "<prefix><red>No region named <yellow><region></yellow> was found."),
    REGION_REMOVED("region.removed", "<prefix><green>Region <yellow><region></yellow> removed."),

    REGION_LIST_HEADER("region.list.header", "<prefix><gray>Regions in <yellow><world></yellow> (<count>):"),
    REGION_LIST_ENTRY("region.list.entry", "<gray> • <yellow><region></yellow> <dark_gray>(<gray><shape></gray>, priority <gray><priority></gray><dark_gray>)"),
    REGION_LIST_EMPTY("region.list.empty", "<prefix><gray>No region in this world yet."),

    REGION_INFO_HEADER("region.info.header", "<prefix><gray>Region <yellow><region></yellow>:"),
    REGION_INFO_WORLD("region.info.world", "<gray> World: <yellow><world></yellow>"),
    REGION_INFO_SHAPE("region.info.shape", "<gray> Shape: <yellow><shape></yellow>"),
    REGION_INFO_PRIORITY("region.info.priority", "<gray> Priority: <yellow><priority></yellow>"),
    REGION_INFO_MEMBERS("region.info.members", "<gray> Members: <yellow><members></yellow>"),
    REGION_INFO_FLAGS("region.info.flags", "<gray> Flags: <yellow><flags></yellow>"),

    FLAG_SET("flag.set", "<prefix><green>Flag <yellow><flag></yellow> set to <yellow><value></yellow> for <yellow><target></yellow> in <yellow><region></yellow>."),
    FLAG_UNKNOWN("flag.unknown", "<prefix><red>Unknown flag <yellow><flag></yellow>."),
    FLAG_VALUE_INVALID("flag.invalid-value", "<prefix><red>Invalid value <yellow><value></yellow> for flag <yellow><flag></yellow>."),

    ACTION_DENIED("protection.denied", "<prefix><red>You can't do that here."),

    RELOADED("commands.reloaded", "<prefix><green>Configuration and messages reloaded.");

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
