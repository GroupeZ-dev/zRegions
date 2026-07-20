package fr.maxlego08.zregions.bukkit.command;

/**
 * A platform-specific Brigadier command binding — the native Paper registrar or the
 * commodore completion layer. Both only <em>bind</em>: every decision (routing,
 * permissions, execution, tab-complete data) stays in the common
 * {@code RegionCommandManager}; these just build a Brigadier tree that delegates to it.
 *
 * <p>Loaded reflectively (the Paper implementation lives in the isolated {@code paper}
 * sourceSet and is only referenced through this interface, so the JVM never links its
 * Paper types on a Spigot server) — the same pattern as the Folia scheduler adapter.</p>
 */
public interface PlatformCommands {

    /** Registers the Brigadier tree(s) for {@code /region}, {@code /rg} and {@code /zregions}. */
    void register();
}
