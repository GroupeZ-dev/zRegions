package fr.maxlego08.zregions.common.locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.junit.jupiter.api.Test;
// Palette is in the same package (fr.maxlego08.zregions.common.locale) — no import needed

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the message palette: the semantic tags must resolve to the fixed hex
 * colours and, crucially, <em>close</em> correctly (a {@code </error>} reverts to
 * the surrounding colour). Also renders every built-in default so a malformed tag
 * in the enum is caught at build time rather than on a live server.
 */
class MessageServiceTest {

    private final MessageService messages = new MessageService();

    @Test
    void semanticTagsResolveToThePaletteHex() {
        List<Segment> segments = flatten(messages.formatRaw("<primary>a<accent>b<success>c"));
        assertEquals(Palette.PRIMARY, colorOf(segments, "a"));
        assertEquals(Palette.ACCENT, colorOf(segments, "b"));
        assertEquals(Palette.SUCCESS, colorOf(segments, "c"));
    }

    @Test
    void closingASemanticTagRevertsToTheSurroundingColour() {
        // "<body>a<error>b</error>c" — the c must be body again, not error
        List<Segment> segments = flatten(messages.formatRaw("<body>a<error>b</error>c"));
        assertEquals(Palette.BODY, colorOf(segments, "a"));
        assertEquals(Palette.ERROR, colorOf(segments, "b"));
        assertEquals(Palette.BODY, colorOf(segments, "c"), "the closing </error> must pop back to body");
    }

    @Test
    void aConfiguredPaletteReplacesTheColours() {
        Palette custom = new Palette(
                TextColor.color(0x111111), TextColor.color(0x222222), TextColor.color(0x333333),
                TextColor.color(0x444444), TextColor.color(0x555555), TextColor.color(0x666666));
        messages.setPalette(custom);
        List<Segment> segments = flatten(messages.formatRaw("<primary>a<accent>b<muted>c"));
        assertEquals(TextColor.color(0x111111), colorOf(segments, "a"));
        assertEquals(TextColor.color(0x222222), colorOf(segments, "b"));
        assertEquals(TextColor.color(0x666666), colorOf(segments, "c"));
    }

    @Test
    void anInteractiveFlagComponentSurvivesInsertionIntoAMessage() {
        // the flag messages inject a clickable/hoverable <flag> as a component placeholder
        Component link = Component.text("pvp", Palette.ACCENT)
                .clickEvent(ClickEvent.suggestCommand("/rg flag spawn pvp "));
        Component rendered = messages.format(Message.FLAG_SET, Placeholder.component("flag", link),
                "value", "deny", "target", "ALL", "region", "spawn");

        List<Segment> segments = flatten(rendered);
        assertEquals(Palette.ACCENT, colorOf(segments, "pvp"), "the flag keeps its accent colour");
        assertTrue(hasSuggestClick(rendered, "/rg flag spawn pvp "), "the flag keeps its click command");
    }

    private static boolean hasSuggestClick(Component component, String command) {
        ClickEvent click = component.clickEvent();
        if (click != null && click.action() == ClickEvent.Action.SUGGEST_COMMAND && click.value().equals(command)) {
            return true;
        }
        return component.children().stream().anyMatch(child -> hasSuggestClick(child, command));
    }

    @Test
    void everyBuiltInMessageRendersWithoutThrowing() {
        for (Message message : Message.values()) {
            assertNotNull(messages.format(message), message.name());
        }
    }

    @Test
    void flagDescriptionFallsBackToTheEnglishDefault() {
        // no language adapter loaded -> the FlagDescriptions fallback is used
        assertEquals("Prevents player-vs-player combat, projectiles included.", messages.flagDescription("pvp"));
        assertEquals("", messages.flagDescription("no-such-flag"));
    }

    private record Segment(String text, TextColor color) {
    }

    private static TextColor colorOf(List<Segment> segments, String text) {
        return segments.stream().filter(s -> s.text().equals(text)).map(Segment::color).findFirst().orElseThrow();
    }

    /** Depth-first leaf text with its effective (inherited) colour. */
    private static List<Segment> flatten(Component component) {
        List<Segment> out = new ArrayList<>();
        collect(component, null, out);
        return out;
    }

    private static void collect(Component component, TextColor inherited, List<Segment> out) {
        TextColor color = component.color() != null ? component.color() : inherited;
        if (component instanceof TextComponent text && !text.content().isEmpty()) {
            out.add(new Segment(text.content(), color));
        }
        for (Component child : component.children()) {
            collect(child, color, out);
        }
    }
}
