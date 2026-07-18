package fr.maxlego08.zregions.common.flag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The comma-separated list flag: parsing, trimming, round-trip stability
 * (serialize output must re-parse identically — the DB load path depends on it)
 * and immutability of the parsed value (read concurrently from the flag map).
 */
class StringListFlagTest {

    private final StringListFlag flag = new StringListFlag("command-blacklist", List.of());

    @Test
    void parsesCommaSeparatedEntriesTrimmed() {
        assertEquals(List.of("tp", "home", "sethome"),
                flag.parse("tp, home ,sethome").orElseThrow());
    }

    @Test
    void singleEntryWorks() {
        assertEquals(List.of("spawn"), flag.parse("spawn").orElseThrow());
    }

    @Test
    void emptyEntriesAreDropped() {
        assertEquals(List.of("tp"), flag.parse(",,tp,,").orElseThrow());
    }

    @Test
    void inputWithoutAnyEntryIsInvalid() {
        assertTrue(flag.parse("").isEmpty());
        assertTrue(flag.parse("  ,  , ").isEmpty());
        assertTrue(flag.parse(null).isEmpty());
    }

    @Test
    void serializeThenParseRoundTrips() {
        List<String> value = List.of("tp", "home", "back");
        Optional<List<String>> reparsed = flag.parse(flag.serialize(value));
        assertEquals(value, reparsed.orElseThrow());
    }

    @Test
    void parsedListIsImmutable() {
        List<String> value = flag.parse("tp, home").orElseThrow();
        assertThrows(UnsupportedOperationException.class, () -> value.add("spawn"));
    }

    @Test
    void defaultValueIsImmutable() {
        assertThrows(UnsupportedOperationException.class, () -> flag.getDefaultValue().add("spawn"));
    }
}
