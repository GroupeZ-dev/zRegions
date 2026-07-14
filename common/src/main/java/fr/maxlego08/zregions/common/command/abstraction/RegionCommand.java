package fr.maxlego08.zregions.common.command.abstraction;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.util.List;
import java.util.Optional;

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

    /** Reminds the sender how to use this command (rendered as a help entry line). */
    protected void sendUsage(ZRegionsPlugin plugin, RegionSender sender) {
        plugin.getMessages().send(sender, Message.HELP_ENTRY, "usage", this.usage, "description", this.usage);
    }

    /** Finds a region by name across all worlds, case-insensitively. */
    protected Optional<Region> findRegion(ZRegionsPlugin plugin, String name) {
        return plugin.getRegionManager().getRegions().stream()
                .filter(region -> region.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    /** Completions over the names of all known regions. */
    protected CompletionSupplier regionNames(ZRegionsPlugin plugin) {
        return CompletionSupplier.startsWith(() -> plugin.getRegionManager().getRegions().stream().map(Region::getName));
    }
}
