package fr.maxlego08.zregions.common.command.commands;

import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import fr.maxlego08.zregions.common.command.tabcomplete.CompletionSupplier;
import fr.maxlego08.zregions.common.command.tabcomplete.TabCompleter;
import fr.maxlego08.zregions.common.command.util.ArgumentList;
import fr.maxlego08.zregions.common.importer.ImportReport;
import fr.maxlego08.zregions.common.importer.WorldGuardImporter;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.common.sender.RegionSender;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Imports regions from another protection plugin's data files. Only WorldGuard
 * for now ({@code plugins/WorldGuard/worlds/<world>/regions.yml}, read directly —
 * WorldGuard does not need to be installed). {@code --dry-run} parses and reports
 * without writing anything. Runs on the async pool like every command, so the
 * file I/O never blocks the game thread; the chat gets a summary, the server
 * log every skipped detail.
 */
public class ImportCommand extends RegionCommand {

    public ImportCommand() {
        super("import", "zregions.admin", "import <worldguard> [--dry-run]", "import");
    }

    @Override
    public void execute(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        Optional<String> optionalSource = args.getOpt(1);
        if (optionalSource.isEmpty()) {
            sendUsage(plugin, sender);
            return;
        }
        if (!optionalSource.get().equalsIgnoreCase(WorldGuardImporter.SOURCE_NAME)) {
            plugin.getMessages().send(sender, Message.IMPORT_UNKNOWN_SOURCE,
                    "source", optionalSource.get(),
                    "sources", WorldGuardImporter.SOURCE_NAME);
            return;
        }
        // a typo'd --dry-run must never silently run the real bulk import
        Optional<String> option = args.getOpt(2);
        if (option.isPresent() && !option.get().equalsIgnoreCase("--dry-run")) {
            sendUsage(plugin, sender);
            return;
        }
        boolean dryRun = option.isPresent();

        Path pluginsDirectory = plugin.getBootstrap().getDataDirectory().toAbsolutePath().getParent();
        Path worldsDirectory = pluginsDirectory == null ? null
                : pluginsDirectory.resolve("WorldGuard").resolve("worlds");
        if (worldsDirectory == null || !Files.isDirectory(worldsDirectory)) {
            plugin.getMessages().send(sender, Message.IMPORT_SOURCE_NOT_FOUND,
                    "source", "WorldGuard",
                    "path", String.valueOf(worldsDirectory));
            return;
        }

        plugin.getMessages().send(sender, Message.IMPORT_STARTED, "source", "WorldGuard");
        ImportReport report = new WorldGuardImporter(plugin).importAll(worldsDirectory, dryRun);

        report.getDetails().forEach(line -> plugin.getLogger().warn("[import] " + line));
        plugin.getMessages().send(sender, Message.IMPORT_DONE,
                "imported", String.valueOf(report.getRegionsImported()),
                "skipped", String.valueOf(report.getRegionsSkipped()),
                "flags", String.valueOf(report.getFlagsApplied()),
                "members", String.valueOf(report.getMembersImported()));
        if (dryRun) {
            plugin.getMessages().send(sender, Message.IMPORT_DRY_RUN);
        }
    }

    @Override
    public List<String> tabComplete(ZRegionsPlugin plugin, RegionSender sender, ArgumentList args) {
        return TabCompleter.create()
                .at(1, CompletionSupplier.startsWith(() -> Stream.of(WorldGuardImporter.SOURCE_NAME)))
                .at(2, CompletionSupplier.startsWith(() -> Stream.of("--dry-run")))
                .complete(args);
    }
}
