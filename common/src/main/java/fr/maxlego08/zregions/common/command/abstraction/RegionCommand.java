package fr.maxlego08.zregions.common.command.abstraction;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.selection.SelectionShapeBuilder;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * A {@code /rg} sub-command, written once in the common module (LuckPerms' Command
 * model): platforms only bind their native executor to the RegionCommandManager.
 *
 * <p>The {@link ArgumentList} handed to {@link #execute} and {@link #tabComplete}
 * is the full argument line, sub-command name included at index 0 — command
 * parameters therefore start at index 1.</p>
 */
public abstract class RegionCommand {

    private final String name;
    private final String permission;
    private final String usage;
    private final String descriptionKeySuffix;

    protected RegionCommand(String name, String permission, String usage, String descriptionKeySuffix) {
        this.name = name;
        this.permission = permission;
        this.usage = usage;
        this.descriptionKeySuffix = descriptionKeySuffix;
    }

    public String getName() {
        return this.name;
    }

    /** The required permission node, or {@code null} when none is required. */
    public String getPermission() {
        return this.permission;
    }

    public String getUsage() {
        return this.usage;
    }

    /** Suffix of the locale key carrying this command's description (reserved for i18n). */
    public String getDescriptionKeySuffix() {
        return this.descriptionKeySuffix;
    }

    public abstract void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args);

    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return List.of();
    }

    public boolean isAuthorized(RegionSender sender) {
        return this.permission == null || sender.hasPermission(this.permission);
    }

    /** The localized description of this command (language file), falling back to its usage. */
    public String getDescription(ZRegionsPlugin plugin) {
        return plugin.getMessages().rawPath("commands.descriptions." + this.descriptionKeySuffix, this.usage);
    }

    /** Reminds the sender how to use this command (rendered as a help entry line). */
    protected void sendUsage(ZRegionsPlugin plugin, RegionSender sender) {
        plugin.getMessages().send(sender, Message.HELP_ENTRY,
                "usage", this.usage,
                "description", getDescription(plugin));
    }

    /** Maps a shape-building failure to its user-facing message (create/redefine). */
    protected void sendSelectionError(ZRegionsPlugin plugin, RegionSender sender, SelectionShapeBuilder.Error error) {
        Message message = switch (error) {
            case INCOMPLETE -> Message.SELECTION_INCOMPLETE;
            case WORLD_MISMATCH -> Message.SELECTION_WORLD_MISMATCH;
            case RADIUS_TOO_SMALL -> Message.SELECTION_RADIUS_TOO_SMALL;
            case POINTS_NEEDED -> Message.SELECTION_POINTS_NEEDED;
        };
        plugin.getMessages().send(sender, message);
    }

    /** The shape display name of a region — the global region has no shape. */
    protected static String shapeName(Region region) {
        return region.getShape() == null ? "GLOBAL" : region.getShape().getType().name();
    }

    /** Quiet lookup for tab completion: first case-insensitive match across all worlds. */
    protected Optional<Region> findRegion(ZRegionsPlugin plugin, String name) {
        return plugin.getRegionManager().getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /**
     * Resolves a region for command execution, disambiguating same-named regions
     * across worlds: an explicit {@code world:name} wins, then a match in the
     * sender's own world, then a unique cross-world match. On failure this sends
     * REGION_NOT_FOUND or REGION_AMBIGUOUS itself and returns empty — callers
     * just return.
     */
    protected Optional<Region> resolveRegion(ZRegionsPlugin plugin, RegionSender sender, String input) {
        int colon = input.indexOf(':');
        if (colon > 0 && colon < input.length() - 1) {
            Optional<Region> exact = plugin.getRegionManager()
                    .getRegion(input.substring(0, colon), input.substring(colon + 1));
            if (exact.isPresent()) {
                return exact;
            }
        }

        List<Region> matches = plugin.getRegionManager().getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(input))
                .toList();
        if (matches.isEmpty()) {
            plugin.getMessages().send(sender, Message.REGION_NOT_FOUND, "region", input);
            return Optional.empty();
        }

        Optional<RegionPlayer> player = sender.asPlayer();
        if (player.isPresent()) {
            for (Region region : matches) {
                if (region.getWorldName().equals(player.get().getWorldName())) {
                    return Optional.of(region);
                }
            }
        }
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }

        plugin.getMessages().send(sender, Message.REGION_AMBIGUOUS,
                "region", input,
                "worlds", matches.stream().map(Region::getWorldName).collect(Collectors.joining(", ")));
        return Optional.empty();
    }

    /** Completions over the names of all known regions. */
    protected CompletionSupplier regionNames(ZRegionsPlugin plugin) {
        return CompletionSupplier.startsWith(() -> plugin.getRegionManager().getRegions().stream().map(Region::getName));
    }
}
