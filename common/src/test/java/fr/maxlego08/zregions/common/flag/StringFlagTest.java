package fr.maxlego08.zregions.common.flag;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StringFlagTest {

    @Test
    void parseReturnsInputAsIs() {
        StringFlag flag = new StringFlag("greeting", "");
        assertEquals(Optional.of("<green>Welcome <player>!"), flag.parse("<green>Welcome <player>!"));
        assertEquals(Optional.of("plain text"), flag.parse("plain text"));
    }

    @Test
    void parseAcceptsEmptyString() {
        StringFlag flag = new StringFlag("greeting", "");
        assertEquals(Optional.of(""), flag.parse(""), "empty string is a valid (blank) value");
    }

    @Test
    void parseNullIsEmpty() {
        StringFlag flag = new StringFlag("greeting", "");
        assertTrue(flag.parse(null).isEmpty());
    }

    @Test
    void serializeIsIdentity() {
        StringFlag flag = new StringFlag("farewell", "");
        assertEquals("<red>Bye <player>", flag.serialize("<red>Bye <player>"));
        assertEquals("", flag.serialize(""));
    }

    @Test
    void defaultValueIsRespected() {
        assertEquals("", new StringFlag("greeting", "").getDefaultValue());
        assertEquals("hello", new StringFlag("motd", "hello").getDefaultValue());
    }

    @Test
    void keyIsExposed() {
        assertEquals("greeting", new StringFlag("greeting", "").getKey());
    }
}
