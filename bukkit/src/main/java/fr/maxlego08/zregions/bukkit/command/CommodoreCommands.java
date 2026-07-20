package fr.maxlego08.zregions.bukkit.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.command.abstraction.RegionCommand;
import me.lucko.commodore.Commodore;
import me.lucko.commodore.CommodoreProvider;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Spigot Brigadier layer: attaches a completion tree (sub-command literals + an
 * {@code ASK_SERVER} argument tail) to the existing Bukkit command via commodore, so
 * clients get native sub-command completion. Execution and argument suggestions stay
 * in {@code BukkitCommandExecutor} — commodore only enriches the client-side tree.
 *
 * <p>Degrades cleanly: if commodore is unsupported on this server the plugin keeps its
 * plain Bukkit tab-completion, nothing breaks. Brigadier is provided by the server
 * (present on Spigot since 1.13), commodore is shaded and relocated.</p>
 */
public final class CommodoreCommands implements PlatformCommands {

    private static final String[] ROOTS = {"region", "zregions"};

    private final ZRegionsBukkitPlugin plugin;

    public CommodoreCommands(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        if (!CommodoreProvider.isSupported()) {
            this.plugin.getLogger().info(
                    "Brigadier completions unavailable here (commodore) — using standard Bukkit tab-completion.");
            return;
        }
        JavaPlugin loader = this.plugin.getBootstrap().getLoader();
        Commodore commodore = CommodoreProvider.getCommodore(loader);
        for (String root : ROOTS) {
            PluginCommand command = loader.getCommand(root);
            if (command != null) {
                commodore.register(command, buildTree(root));
            }
        }
        this.plugin.getLogger().info("Registered Brigadier command completions via commodore.");
    }

    private LiteralCommandNode<Object> buildTree(String rootName) {
        RegionCommandManager manager = this.plugin.getCommandManager();
        LiteralArgumentBuilder<Object> root = LiteralArgumentBuilder.literal(rootName);
        for (RegionCommand sub : manager.getCommands()) {
            root.then(subCommand(sub.getName()));
            for (String alias : sub.getAliases()) {
                root.then(subCommand(alias));
            }
        }
        // Any unrecognised first token: let the client accept it and ask the server,
        // so the plugin's own "unknown sub-command" message still shows.
        root.then(RequiredArgumentBuilder.<Object, String>argument("input", StringArgumentType.greedyString())
                .suggests((context, builder) -> builder.buildFuture()));
        return root.build();
    }

    /** A sub-command literal with a greedy tail whose suggestion provider marks it ASK_SERVER. */
    private static LiteralArgumentBuilder<Object> subCommand(String name) {
        return LiteralArgumentBuilder.<Object>literal(name)
                .then(RequiredArgumentBuilder.<Object, String>argument("args", StringArgumentType.greedyString())
                        .suggests((context, builder) -> builder.buildFuture()));
    }
}
