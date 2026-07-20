package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The location flag: exact serialize/parse round-trip (the DB load path depends
 * on it, incl. fractional coordinates and non-zero yaw/pitch), a null default,
 * a null-safe serialize and rejection of malformed input.
 */
class LocationFlagTest {

    private final LocationFlag flag = new LocationFlag("teleport");

    @Test
    void serializeThenParseRoundTripsEveryField() {
        RegionLocation value = new RegionLocation("world", 12.5, 64.0, -8.25, 90.5f, -12.0f);
        RegionLocation reparsed = this.flag.parse(this.flag.serialize(value)).orElseThrow();

        assertEquals("world", reparsed.getWorldName());
        assertEquals(12.5, reparsed.getX(), 0.0);
        assertEquals(64.0, reparsed.getY(), 0.0);
        assertEquals(-8.25, reparsed.getZ(), 0.0);
        assertEquals(90.5f, reparsed.getYaw(), 0.0f);
        assertEquals(-12.0f, reparsed.getPitch(), 0.0f);
    }

    @Test
    void worldNameContainingTheSeparatorStillRoundTrips() {
        RegionLocation value = new RegionLocation("weird;world", 1.0, 2.0, 3.0);
        RegionLocation reparsed = this.flag.parse(this.flag.serialize(value)).orElseThrow();
        assertEquals("weird;world", reparsed.getWorldName());
        assertEquals(3.0, reparsed.getZ(), 0.0);
    }

    @Test
    void defaultIsNullAndSerializeIsNullSafe() {
        assertNull(this.flag.getDefaultValue());
        assertEquals("", this.flag.serialize(null), "serialize(null) must be blank for the flag editor");
    }

    @Test
    void malformedInputIsRejected() {
        assertTrue(this.flag.parse(null).isEmpty());
        assertTrue(this.flag.parse("").isEmpty());
        assertTrue(this.flag.parse("world;1;2").isEmpty(), "too few fields");
        assertTrue(this.flag.parse("world;x;2;3;0;0").isEmpty(), "non-numeric coordinate");
    }

    @Test
    void blankReparsesToEmpty() {
        Optional<RegionLocation> parsed = this.flag.parse(this.flag.serialize(null));
        assertTrue(parsed.isEmpty());
    }

    @Test
    void keyAndDefaultAreExposed() {
        assertEquals("teleport", this.flag.getKey());
    }
}
