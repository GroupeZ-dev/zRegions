package fr.maxlego08.zregions.common.config;

import fr.maxlego08.zregions.common.locale.Palette;
import net.kyori.adventure.text.format.TextColor;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The {@code messages.palette.*} config parsing: hex acceptance and the per-key
 * fallback that keeps a typo from blanking the messages.
 */
class ZRegionsConfigurationPaletteTest {

    @Test
    void unsetPaletteUsesTheBuiltInDefaults() {
        Palette palette = configWith(Map.of()).getPalette();
        assertEquals(Palette.PRIMARY, palette.primary());
        assertEquals(Palette.MUTED, palette.muted());
    }

    @Test
    void hexColoursAreParsedWithOrWithoutHash() {
        Palette palette = configWith(Map.of(
                "messages.palette.primary", "#ff0000",
                "messages.palette.accent", "00ff00")).getPalette();
        assertEquals(TextColor.color(0xFF0000), palette.primary());
        assertEquals(TextColor.color(0x00FF00), palette.accent());
    }

    @Test
    void invalidOrEmptyHexFallsBackToTheDefault() {
        Palette palette = configWith(Map.of(
                "messages.palette.error", "not-a-colour",
                "messages.palette.body", "   ")).getPalette();
        assertEquals(Palette.ERROR, palette.error());
        assertEquals(Palette.BODY, palette.body());
    }

    @Test
    void nonSixDigitHexFallsBackInsteadOfGuessing() {
        // #fff must NOT silently become #000FFF (Adventure parses "fff" as 0xFFF)
        Palette palette = configWith(Map.of(
                "messages.palette.success", "#fff",
                "messages.palette.primary", "#1234567")).getPalette();
        assertEquals(Palette.SUCCESS, palette.success());
        assertEquals(Palette.PRIMARY, palette.primary());
    }

    private static ZRegionsConfiguration configWith(Map<String, String> values) {
        return new ZRegionsConfiguration(new MapConfigurationAdapter(values));
    }

    private record MapConfigurationAdapter(Map<String, String> values) implements ConfigurationAdapter {
        @Override
        public String getString(String path, String def) {
            return values.getOrDefault(path, def);
        }

        @Override
        public int getInt(String path, int def) {
            return def;
        }

        @Override
        public double getDouble(String path, double def) {
            return def;
        }

        @Override
        public boolean getBoolean(String path, boolean def) {
            return def;
        }

        @Override
        public List<String> getStringList(String path, List<String> def) {
            return def;
        }

        @Override
        public Collection<String> getKeys(String path) {
            return List.of();
        }

        @Override
        public void reload() {
        }
    }
}
