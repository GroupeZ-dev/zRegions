package fr.maxlego08.zregions.common.locale;

import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.flag.FlagDescriptions;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.sender.RegionSender;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * Produces Adventure Components from MiniMessage strings, per language.
 * Sending is delegated to the platform via RegionSender/RegionPlayer — this class
 * never touches a platform API.
 *
 * <p>Every rendered message carries the {@link Palette} semantic-colour tags
 * ({@code <primary>}, {@code <accent>}, {@code <success>}…), so message strings pick
 * their colour by role and the theme is tuned in one place.</p>
 */
public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile ConfigurationAdapter adapter;
    private volatile Palette palette = Palette.DEFAULT;

    /** Loads (or reloads) the language file backing the messages. May be null in tests. */
    public void load(ConfigurationAdapter adapter) {
        this.adapter = adapter;
    }

    /** Swaps in the configured colour palette (refreshed on reload). */
    public void setPalette(Palette palette) {
        this.palette = palette;
    }

    /** The active palette — for Components built in Java (e.g. pagination arrows). */
    public Palette palette() {
        return this.palette;
    }

    public String raw(Message message) {
        ConfigurationAdapter current = this.adapter;
        return current == null ? message.getDefault() : current.getString(message.getPath(), message.getDefault());
    }

    /**
     * A raw language-file string by dynamic path (e.g. per-command descriptions,
     * {@code commands.descriptions.<name>}), falling back to {@code def}.
     */
    public String rawPath(String path, String def) {
        ConfigurationAdapter current = this.adapter;
        return current == null ? def : current.getString(path, def);
    }

    /**
     * The localized plain-text description of a flag ({@code flags.<key>}), falling
     * back to its {@link FlagDescriptions English default}. Plain text — the caller
     * colours it (hover in {@code /rg flags}, lore in the GUI).
     */
    public String flagDescription(String flagKey) {
        return rawPath("flags." + flagKey, FlagDescriptions.get(flagKey));
    }

    /** prefix + palette + the (key,value) pairs as unparsed placeholders. */
    private TagResolver.Builder baseResolvers(String... placeholders) {
        TagResolver.Builder resolvers = TagResolver.builder();
        resolvers.resolver(this.palette.resolver());
        resolvers.resolver(Placeholder.parsed("prefix", raw(Message.PREFIX)));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return resolvers;
    }

    /**
     * Formats a message. {@code placeholders} are (key, value) pairs, inserted as
     * unparsed text (safe for player-provided input such as region names).
     */
    public Component format(Message message, String... placeholders) {
        return this.miniMessage.deserialize(raw(message), baseResolvers(placeholders).build());
    }

    /**
     * Formats a message with an extra resolver on top of the usual ones — typically a
     * {@code Placeholder.component(key, …)} that injects an interactive (hover/click)
     * Component at a placeholder position, e.g. a clickable flag name in the flag messages.
     */
    public Component format(Message message, TagResolver extra, String... placeholders) {
        return this.miniMessage.deserialize(raw(message), baseResolvers(placeholders).resolver(extra).build());
    }

    /**
     * Formats a raw MiniMessage string that does not come from the language file
     * (e.g. a greeting/farewell flag value). Same placeholder contract as
     * {@link #format}; the {@code <prefix>} tag and palette resolve too.
     */
    public Component formatRaw(String miniMessageText, String... placeholders) {
        return this.miniMessage.deserialize(miniMessageText, baseResolvers(placeholders).build());
    }

    public void send(RegionSender sender, Message message, String... placeholders) {
        sender.sendMessage(format(message, placeholders));
    }

    public void send(RegionPlayer player, Message message, String... placeholders) {
        player.sendMessage(format(message, placeholders));
    }
}
