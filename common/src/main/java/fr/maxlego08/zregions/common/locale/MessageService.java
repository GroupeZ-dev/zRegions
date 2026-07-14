package fr.maxlego08.zregions.common.locale;

import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
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
 */
public final class MessageService {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private volatile ConfigurationAdapter adapter;

    /** Loads (or reloads) the language file backing the messages. May be null in tests. */
    public void load(ConfigurationAdapter adapter) {
        this.adapter = adapter;
    }

    public String raw(Message message) {
        ConfigurationAdapter current = this.adapter;
        return current == null ? message.getDefault() : current.getString(message.getPath(), message.getDefault());
    }

    /**
     * Formats a message. {@code placeholders} are (key, value) pairs, inserted as
     * unparsed text (safe for player-provided input such as region names).
     */
    public Component format(Message message, String... placeholders) {
        TagResolver.Builder resolvers = TagResolver.builder();
        resolvers.resolver(Placeholder.parsed("prefix", raw(Message.PREFIX)));
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            resolvers.resolver(Placeholder.unparsed(placeholders[i], placeholders[i + 1]));
        }
        return this.miniMessage.deserialize(raw(message), resolvers.build());
    }

    public void send(RegionSender sender, Message message, String... placeholders) {
        sender.sendMessage(format(message, placeholders));
    }

    public void send(RegionPlayer player, Message message, String... placeholders) {
        player.sendMessage(format(message, placeholders));
    }
}
