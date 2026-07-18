package fr.maxlego08.zregions.common.movement;

import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.flag.Flags;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.region.TestPluginFixture;
import fr.maxlego08.zregions.common.region.ZRegionManager;
import fr.maxlego08.zregions.common.shape.CuboidShape;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the enter/exit diff of {@link RegionMovementTracker}: greeting/farewell
 * messages, entry/exit enforcement with bypass, arrival/quit lifecycle and the
 * denied-message throttle. Messages are asserted by count only — rendering the
 * MiniMessage output is the {@code MessageService}'s concern, not the tracker's.
 */
class RegionMovementTrackerTest {

    private static final String WORLD = "world";

    private TestPluginFixture plugin;
    private ZRegionManager manager;
    private RegionMovementTracker tracker;
    private FakeRegionPlayer player;
    private Region region;

    @BeforeEach
    void setUp() {
        this.plugin = new TestPluginFixture();
        this.manager = new ZRegionManager(this.plugin);
        this.plugin.setRegionManager(this.manager);
        this.tracker = this.plugin.getMovementTracker();
        this.player = new FakeRegionPlayer();
        this.region = this.manager.createRegion(WORLD, "shop", new CuboidShape(0, 0, 0, 10, 10, 10), 10, null);
    }

    @Test
    void greetingOnEnterAndFarewellOnExit() {
        this.manager.setFlag(this.region, Flags.GREETING, GroupTarget.ALL, "hi <player>");
        this.manager.setFlag(this.region, Flags.FAREWELL, GroupTarget.ALL, "bye <player>");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(1, this.player.messages.size(), "entering must greet exactly once");
        assertNotNull(this.player.messages.get(0));

        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals(2, this.player.messages.size(), "leaving must send exactly one farewell");
        assertNotNull(this.player.messages.get(1));
    }

    @Test
    void regionWithoutMessagesStaysSilent() {
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals(0, this.player.messages.size(), "empty greeting/farewell defaults send nothing");
    }

    @Test
    void entryDenyRefusesTheMoveAndKeepsTheSetUnchanged() {
        this.manager.setFlag(this.region, Flags.ENTRY, GroupTarget.ALL, false);

        assertFalse(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty(), "a denied move must not update the set");
    }

    @Test
    void bypassIgnoresEntryDeny() {
        this.manager.setFlag(this.region, Flags.ENTRY, GroupTarget.ALL, false);

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), true));
        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).contains(this.region.getId()));
    }

    @Test
    void exitDenyKeepsThePlayerInside() {
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false), "entry is free");
        this.manager.setFlag(this.region, Flags.EXIT, GroupTarget.ALL, false);

        assertFalse(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).contains(this.region.getId()), "the set must still contain the region");
    }

    @Test
    void arrivalInitializesTheSetAndGreets() {
        this.manager.setFlag(this.region, Flags.GREETING, GroupTarget.ALL, "hi <player>");

        this.tracker.handleArrival(this.player, at(5, 5, 5));

        assertFalse(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());
        assertEquals(1, this.player.messages.size(), "a join inside the region must greet");
    }

    @Test
    void arrivalOutsideSendsTheFarewell() {
        this.manager.setFlag(this.region, Flags.FAREWELL, GroupTarget.ALL, "bye <player>");
        this.tracker.handleArrival(this.player, at(5, 5, 5));

        // dying inside and respawning outside must say goodbye
        this.tracker.handleArrival(this.player, at(80, 5, 80));

        assertEquals(1, this.player.messages.size(), "leaving through an arrival must send the farewell");
        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());
    }

    @Test
    void seedInitializesSilently() {
        this.manager.setFlag(this.region, Flags.GREETING, GroupTarget.ALL, "hi <player>");

        this.tracker.seed(this.player, at(5, 5, 5));

        assertFalse(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());
        assertEquals(0, this.player.messages.size(), "enable-time seeding must not greet");
    }

    @Test
    void deletedRegionNeverTrapsThePlayer() {
        this.manager.setFlag(this.region, Flags.EXIT, GroupTarget.ALL, false);
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false), "entry is free");

        this.manager.deleteRegion(this.region);

        // the exit=deny region is gone: its stale id must not freeze the player
        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());
    }

    @Test
    void flagsChangedAfterRedefineApplyToTrackedPlayers() {
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));

        // redefine copy-swaps the instance; a flag set AFTER the swap must still be enforced
        this.manager.redefine(this.region, new CuboidShape(0, 0, 0, 10, 10, 10));
        Region live = this.manager.getRegion(this.region.getId()).orElseThrow();
        this.manager.setFlag(live, Flags.EXIT, GroupTarget.ALL, false);

        assertFalse(this.tracker.handleMove(this.player, at(50, 5, 50), false),
                "exit deny set after a copy-swap must still trap the player");
    }

    @Test
    void quitPurgesThePlayerState() {
        this.tracker.handleArrival(this.player, at(5, 5, 5));
        assertFalse(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());

        this.tracker.handleQuit(this.player.getUniqueId());

        assertTrue(this.tracker.getRegionIds(this.player.getUniqueId()).isEmpty());
    }

    @Test
    void moveWithinTheSameRegionSetIsSilent() {
        this.manager.setFlag(this.region, Flags.GREETING, GroupTarget.ALL, "hi <player>");
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(1, this.player.messages.size());

        assertTrue(this.tracker.handleMove(this.player, at(6, 5, 6), false));
        assertEquals(1, this.player.messages.size(), "no boundary crossed: no message");
    }

    @Test
    void deniedMessageIsThrottled() {
        this.manager.setFlag(this.region, Flags.ENTRY, GroupTarget.ALL, false);

        assertFalse(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertFalse(this.tracker.handleMove(this.player, at(6, 5, 6), false));

        assertEquals(1, this.player.messages.size(), "two immediate denials must send a single message");
    }

    private static RegionLocation at(double x, double y, double z) {
        return new RegionLocation(WORLD, x, y, z);
    }

    /** Records every component sent to it; the location follows {@link #teleport}. */
    private static final class FakeRegionPlayer implements RegionPlayer {

        private final UUID uniqueId = UUID.randomUUID();
        private final List<Component> messages = new ArrayList<>();
        private RegionLocation location = at(0, 64, 0);

        @Override
        public UUID getUniqueId() {
            return this.uniqueId;
        }

        @Override
        public String getName() {
            return "Notch";
        }

        @Override
        public String getWorldName() {
            return this.location.getWorldName();
        }

        @Override
        public RegionLocation getLocation() {
            return this.location;
        }

        @Override
        public boolean hasPermission(String permission) {
            return false;
        }

        @Override
        public void sendMessage(Component message) {
            this.messages.add(message);
        }

        @Override
        public void teleport(RegionLocation location) {
            this.location = location;
        }

        @Override
        public void spawnBorderParticle(double x, double y, double z) {
        }

        @Override
        public void giveWand() {
        }

        @Override
        public boolean isOnline() {
            return true;
        }
    }
}
