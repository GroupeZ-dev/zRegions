package fr.maxlego08.zregions.common.locale;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * The zRegions message palette: a small set of high-contrast hex colours exposed
 * two ways — as {@link TextColor} constants (for Components built in Java, e.g.
 * interactive command lines) and as MiniMessage <em>semantic tags</em>
 * ({@code <primary>}, {@code <accent>}, {@code <success>}, {@code <error>},
 * {@code <body>}, {@code <muted>}) via {@link #resolver()}, so message strings never
 * hard-code raw hex and the whole theme is retuned here in one place.
 *
 * <p>Deliberately avoids the vanilla named colours (which vary wildly between
 * clients/resource packs); every value is a fixed hex chosen for contrast on a
 * dark chat background.</p>
 */
public final class Palette {

    /** Brand blue — headers, titles, the plugin identity (matches the prefix gradient). */
    public static final TextColor PRIMARY = TextColor.color(0x38BDF8);
    /** Warm amber — values, names, anything the reader should focus on. */
    public static final TextColor ACCENT = TextColor.color(0xFBBF24);
    /** Green — confirmations and successful outcomes. */
    public static final TextColor SUCCESS = TextColor.color(0x4ADE80);
    /** Rose — refusals and errors (softer than pure red, still clearly a warning). */
    public static final TextColor ERROR = TextColor.color(0xFB7185);
    /** Light slate — ordinary sentence text. */
    public static final TextColor BODY = TextColor.color(0xCBD5E1);
    /** Slate — punctuation, separators, secondary detail. */
    public static final TextColor MUTED = TextColor.color(0x64748B);

    private static final TagResolver RESOLVER = TagResolver.builder()
            .tag("primary", Tag.styling(PRIMARY))
            .tag("accent", Tag.styling(ACCENT))
            .tag("success", Tag.styling(SUCCESS))
            .tag("error", Tag.styling(ERROR))
            .tag("body", Tag.styling(BODY))
            .tag("muted", Tag.styling(MUTED))
            .build();

    private Palette() {
    }

    /** The semantic-colour tags, resolved into every message the plugin renders. */
    public static TagResolver resolver() {
        return RESOLVER;
    }
}
