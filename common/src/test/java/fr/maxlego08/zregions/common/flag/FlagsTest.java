package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sanity checks on the built-in flag catalog: completeness, key uniqueness,
 * registration into a registry and the documented defaults.
 */
class FlagsTest {

    @Test
    void allContainsEveryBuiltInFlag() {
        assertEquals(66, Flags.all().size());
    }

    @Test
    void everyKeyIsUnique() {
        List<Flag<?>> all = Flags.all();
        Set<String> keys = new HashSet<>();
        for (Flag<?> flag : all) {
            assertTrue(keys.add(flag.getKey()), "duplicate flag key: " + flag.getKey());
        }
        assertEquals(all.size(), keys.size());
    }

    @Test
    void registerAllPutsEveryFlagInTheRegistry() {
        ZFlagRegistry registry = new ZFlagRegistry();
        Flags.registerAll(registry);

        assertEquals(Flags.all().size(), registry.getFlags().size());
        assertSame(Flags.PVP, registry.getFlag("pvp").orElseThrow());
        assertSame(Flags.BLOCK_BREAK, registry.getFlag("block-break").orElseThrow());
        assertSame(Flags.GREETING, registry.getFlag("greeting").orElseThrow());
    }

    @Test
    void registryLookupIsCaseInsensitive() {
        ZFlagRegistry registry = new ZFlagRegistry();
        Flags.registerAll(registry);

        assertSame(Flags.PVP, registry.getFlag("PVP").orElseThrow());
        assertSame(Flags.CONTAINER_ACCESS, registry.getFlag("Container-Access").orElseThrow());
        assertTrue(registry.getFlag("no-such-flag").isEmpty());
    }

    @Test
    void stateFlagDefaultsMatchTheDocumentedModel() {
        assertTrue(Flags.ENTRY.getDefaultValue());
        assertTrue(Flags.EXIT.getDefaultValue());
        assertTrue(Flags.PVP.getDefaultValue());
        assertTrue(Flags.CHAT.getDefaultValue());
        assertTrue(Flags.ELYTRA.getDefaultValue());
        assertTrue(Flags.FLY.getDefaultValue());
        assertTrue(Flags.TOTEM.getDefaultValue());
        assertTrue(Flags.EXP_DROP.getDefaultValue());
        assertTrue(Flags.MOB_DAMAGE.getDefaultValue());
        assertFalse(Flags.INVINCIBLE.getDefaultValue(), "players are vulnerable by default");
        assertFalse(Flags.KEEP_INVENTORY.getDefaultValue(), "death drops everything by default");
    }

    @Test
    void zoneMessageFlagsDefaultToEmpty() {
        assertEquals("", Flags.GREETING.getDefaultValue());
        assertEquals("", Flags.FAREWELL.getDefaultValue());
        assertEquals("", Flags.TITLE.getDefaultValue());
        assertEquals("", Flags.SUBTITLE.getDefaultValue());
        assertEquals("", Flags.ACTION_BAR.getDefaultValue());
    }

    @Test
    void commandBlacklistDefaultsToAnEmptyList() {
        assertTrue(Flags.COMMAND_BLACKLIST.getDefaultValue().isEmpty(), "no command is blocked by default");
    }
}
