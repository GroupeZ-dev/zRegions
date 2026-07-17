package fr.maxlego08.zregions.common.storage;

import fr.maxlego08.zregions.common.config.ConfigurationAdapter;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Integration tests for {@link SarahRegionStorage} against a real SQLite database in a
 * temporary directory (sqlite-jdbc is on the test runtime classpath). The default
 * configuration values already select SQLITE and the "zregions_" prefix.
 */
class SarahRegionStorageTest {

    private static final PluginLogger NO_OP_LOGGER = new PluginLogger() {
        @Override
        public void info(String message) {
        }

        @Override
        public void warn(String message) {
        }

        @Override
        public void warn(String message, Throwable throwable) {
        }

        @Override
        public void severe(String message) {
        }

        @Override
        public void severe(String message, Throwable throwable) {
        }
    };

    @TempDir
    Path tempDir;

    private SarahRegionStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        this.storage = new SarahRegionStorage(sqliteConfiguration(), this.tempDir, NO_OP_LOGGER);
        this.storage.connect();
    }

    @AfterEach
    void tearDown() {
        this.storage.disconnect();
    }

    @Test
    void loadRegionsFiltersByOriginServer() {
        StoredRegion global = region(UUID.randomUUID(), "spawn", ZRegionsConfiguration.GLOBAL_SERVER);
        StoredRegion survival = region(UUID.randomUUID(), "mine", "survival");
        this.storage.saveRegion(global);
        this.storage.saveRegion(survival);
        this.storage.saveFlag(global.id(), "pvp", "ALL", "false");
        this.storage.saveMember(survival.id(), UUID.randomUUID(), "OWNER");

        List<StoredRegion> survivalRegions = this.storage.loadRegions("survival");
        assertEquals(2, survivalRegions.size());
        assertEquals(Set.of(global.id(), survival.id()),
                survivalRegions.stream().map(StoredRegion::id).collect(Collectors.toSet()));

        List<StoredRegion> otherRegions = this.storage.loadRegions("autre");
        assertEquals(1, otherRegions.size());
        assertEquals(global.id(), otherRegions.get(0).id());
    }

    @Test
    void loadRegionRoundTripsEveryField() {
        UUID id = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID memberId = UUID.randomUUID();
        StoredRegion region = new StoredRegion(id, "arena", "world_nether", 42, "CUBOID",
                "{\"x1\":-5,\"y1\":0,\"z1\":-5,\"x2\":5,\"y2\":64,\"z2\":5}", parentId, true,
                "survival", 7, List.of(), List.of());

        this.storage.saveRegion(region);
        this.storage.saveFlag(id, "pvp", "ALL", "true");
        this.storage.saveFlag(id, "block_break", "MEMBERS", "false");
        this.storage.saveMember(id, ownerId, "OWNER");
        this.storage.saveMember(id, memberId, "MEMBER");

        Optional<StoredRegion> loadedOptional = this.storage.loadRegion(id);
        assertTrue(loadedOptional.isPresent());

        StoredRegion loaded = loadedOptional.get();
        assertEquals(id, loaded.id());
        assertEquals("arena", loaded.name());
        assertEquals("world_nether", loaded.world());
        assertEquals(42, loaded.priority());
        assertEquals("CUBOID", loaded.shapeType());
        assertEquals("{\"x1\":-5,\"y1\":0,\"z1\":-5,\"x2\":5,\"y2\":64,\"z2\":5}", loaded.shapeData());
        assertEquals(parentId, loaded.parentId());
        assertTrue(loaded.global());
        assertEquals("survival", loaded.originServer());
        assertEquals(7, loaded.version());

        assertEquals(2, loaded.flags().size());
        assertTrue(loaded.flags().contains(new StoredRegion.StoredFlag("pvp", "ALL", "true")));
        assertTrue(loaded.flags().contains(new StoredRegion.StoredFlag("block_break", "MEMBERS", "false")));

        assertEquals(2, loaded.members().size());
        assertTrue(loaded.members().contains(new StoredRegion.StoredMember(ownerId, "OWNER")));
        assertTrue(loaded.members().contains(new StoredRegion.StoredMember(memberId, "MEMBER")));
    }

    @Test
    void saveRegionAgainUpdatesWithoutDuplicating() {
        UUID id = UUID.randomUUID();
        this.storage.saveRegion(region(id, "farm", ZRegionsConfiguration.GLOBAL_SERVER));

        StoredRegion updated = new StoredRegion(id, "farm-renamed", "world", 99, "CUBOID",
                "{\"x1\":1}", null, false, ZRegionsConfiguration.GLOBAL_SERVER, 1, List.of(), List.of());
        this.storage.saveRegion(updated);

        List<StoredRegion> regions = this.storage.loadRegions("autre");
        assertEquals(1, regions.size());
        assertEquals("farm-renamed", regions.get(0).name());
        assertEquals(99, regions.get(0).priority());
        assertEquals(1, regions.get(0).version());
        assertNull(regions.get(0).parentId());
    }

    @Test
    void deleteRegionPurgesFlagsAndMembers() {
        UUID id = UUID.randomUUID();
        this.storage.saveRegion(region(id, "doomed", "survival"));
        this.storage.saveFlag(id, "pvp", "ALL", "true");
        this.storage.saveMember(id, UUID.randomUUID(), "OWNER");

        this.storage.deleteRegion(id);
        assertTrue(this.storage.loadRegion(id).isEmpty());

        // re-create the same row: no orphaned children may resurface
        this.storage.saveRegion(region(id, "doomed", "survival"));
        StoredRegion reloaded = this.storage.loadRegion(id).orElseThrow();
        assertTrue(reloaded.flags().isEmpty());
        assertTrue(reloaded.members().isEmpty());
    }

    @Test
    void saveFlagReplacesExistingValue() {
        UUID id = UUID.randomUUID();
        this.storage.saveRegion(region(id, "pvp-zone", "survival"));

        this.storage.saveFlag(id, "pvp", "ALL", "true");
        this.storage.saveFlag(id, "pvp", "ALL", "false");
        this.storage.saveFlag(id, "pvp", "MEMBERS", "true");

        StoredRegion loaded = this.storage.loadRegion(id).orElseThrow();
        assertEquals(2, loaded.flags().size());
        assertTrue(loaded.flags().contains(new StoredRegion.StoredFlag("pvp", "ALL", "false")));
        assertFalse(loaded.flags().contains(new StoredRegion.StoredFlag("pvp", "ALL", "true")));
        assertTrue(loaded.flags().contains(new StoredRegion.StoredFlag("pvp", "MEMBERS", "true")));
    }

    private static StoredRegion region(UUID id, String name, String originServer) {
        return new StoredRegion(id, name, "world", 10, "CUBOID", "{\"x1\":0}", null, false,
                originServer, 0, List.of(), List.of());
    }

    private static ZRegionsConfiguration sqliteConfiguration() {
        return new ZRegionsConfiguration(new ConfigurationAdapter() {
            @Override
            public String getString(String path, String def) {
                return def;
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
                return Collections.emptyList();
            }

            @Override
            public void reload() {
            }
        });
    }
}
