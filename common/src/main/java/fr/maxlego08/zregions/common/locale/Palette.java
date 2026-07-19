package fr.maxlego08.zregions.common.locale;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

/**
 * The zRegions message palette: six high-contrast colours addressed by role rather
 * than by name. Exposed two ways — as {@link TextColor} accessors (for Components
 * built in Java, e.g. the pagination arrows) and as MiniMessage <em>semantic tags</em>
 * ({@code <primary>}, {@code <accent>}, {@code <success>}, {@code <error>},
 * {@code <body>}, {@code <muted>}) via {@link #resolver()}, so message strings never
 * hard-code raw hex.
 *
 * <p>A palette is immutable; the built-in {@link #DEFAULT} (Sky &amp; Amber) is used
 * until the server's configured one is loaded (see {@code messages.palette.*} in
 * config.yml). Deliberately avoids the vanilla named colours, which vary between
 * clients and resource packs.</p>
 */
public final class Palette {

    /** Default "Sky &amp; Amber" values — also the per-key fallback for an invalid config colour. */
    public static final TextColor PRIMARY = TextColor.color(0x38BDF8);
    public static final TextColor ACCENT = TextColor.color(0xFBBF24);
    public static final TextColor SUCCESS = TextColor.color(0x4ADE80);
    public static final TextColor ERROR = TextColor.color(0xFB7185);
    public static final TextColor BODY = TextColor.color(0xCBD5E1);
    public static final TextColor MUTED = TextColor.color(0x64748B);

    /** The built-in palette, used before the configured one loads (and in tests). */
    public static final Palette DEFAULT = new Palette(PRIMARY, ACCENT, SUCCESS, ERROR, BODY, MUTED);

    private final TextColor primary;
    private final TextColor accent;
    private final TextColor success;
    private final TextColor error;
    private final TextColor body;
    private final TextColor muted;
    private final TagResolver resolver;

    public Palette(TextColor primary, TextColor accent, TextColor success, TextColor error,
                   TextColor body, TextColor muted) {
        this.primary = primary;
        this.accent = accent;
        this.success = success;
        this.error = error;
        this.body = body;
        this.muted = muted;
        this.resolver = TagResolver.builder()
                .tag("primary", Tag.styling(primary))
                .tag("accent", Tag.styling(accent))
                .tag("success", Tag.styling(success))
                .tag("error", Tag.styling(error))
                .tag("body", Tag.styling(body))
                .tag("muted", Tag.styling(muted))
                .build();
    }

    public TextColor primary() {
        return this.primary;
    }

    public TextColor accent() {
        return this.accent;
    }

    public TextColor success() {
        return this.success;
    }

    public TextColor error() {
        return this.error;
    }

    public TextColor body() {
        return this.body;
    }

    public TextColor muted() {
        return this.muted;
    }

    /** The semantic-colour tags for MiniMessage, built once for this palette. */
    public TagResolver resolver() {
        return this.resolver;
    }
}
