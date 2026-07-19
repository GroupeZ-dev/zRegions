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
    void entryDenyMessageOverridesTheGenericRefusal() {
        this.manager.setFlag(this.region, Flags.ENTRY, GroupTarget.ALL, false);
        this.manager.setFlag(this.region, Flags.ENTRY_DENY_MESSAGE, GroupTarget.ALL, "keep out <player>");

        assertFalse(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(1, this.player.messages.size(), "a custom entry-deny-message is sent on refusal");
    }

    @Test
    void farewellTitleShownOnExit() {
        this.manager.setFlag(this.region, Flags.FAREWELL_TITLE, GroupTarget.ALL, "bye");
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false), "entry is free");
        assertTrue(this.player.titles.isEmpty(), "no title on enter when only the farewell title is set");

        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals(1, this.player.titles.size(), "leaving shows the farewell title once");
    }

    @Test
    void walkSpeedAppliedOnEnterAndRestoredOnExit() {
        this.manager.setFlag(this.region, Flags.WALK_SPEED, GroupTarget.ALL, 0.5);

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(0.5f, this.player.walkSpeed, 1e-6f, "walk-speed applied on enter");

        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals(0.2f, this.player.walkSpeed, 1e-6f, "walk-speed restored to default on exit");
    }

    @Test
    void gamemodeAppliedOnEnterAndOriginalRestoredOnExit() {
        this.manager.setFlag(this.region, Flags.GAMEMODE, GroupTarget.ALL, "adventure");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals("adventure", this.player.gameMode, "gamemode applied on enter");

        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals("survival", this.player.gameMode, "the original gamemode is restored on exit");
    }

    @Test
    void glowAppliedOnEnterAndClearedOnExit() {
        this.manager.setFlag(this.region, Flags.GLOW, GroupTarget.ALL, true);

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertTrue(this.player.glowing, "glow applied on enter");

        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertFalse(this.player.glowing, "glow cleared on exit");
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
    void titleAndActionBarShowOnEnterOnly() {
        this.manager.setFlag(this.region, Flags.TITLE, GroupTarget.ALL, "Welcome");
        this.manager.setFlag(this.region, Flags.ACTION_BAR, GroupTarget.ALL, "in <region>");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(1, this.player.titles.size(), "entering must show the title once");
        assertEquals(1, this.player.actionBars.size(), "entering must show the action bar once");

        assertTrue(this.tracker.handleMove(this.player, at(6, 5, 6), false));
        assertTrue(this.tracker.handleMove(this.player, at(50, 5, 50), false));
        assertEquals(1, this.player.titles.size(), "moving inside or leaving must not re-show");
        assertEquals(1, this.player.actionBars.size());
    }

    @Test
    void subtitleAloneStillShowsATitle() {
        this.manager.setFlag(this.region, Flags.SUBTITLE, GroupTarget.ALL, "the subtitle");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));

        assertEquals(1, this.player.titles.size(), "a lone subtitle must still be displayed");
        assertEquals(Component.empty(), this.player.titles.get(0)[0], "the main title line stays empty");
        assertNotNull(this.player.titles.get(0)[1]);
    }

    @Test
    void enteringOverlappingRegionsShowsTheHighestPriorityDisplayOnly() {
        // displays are last-write-wins on the client: the priority-10 title must win
        Region town = this.manager.createRegion(WORLD, "town", new CuboidShape(0, 0, 0, 10, 10, 10), 1, null);
        this.manager.setFlag(this.region, Flags.TITLE, GroupTarget.ALL, "Arena");
        this.manager.setFlag(town, Flags.TITLE, GroupTarget.ALL, "Town");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));

        assertEquals(1, this.player.titles.size(), "one title for the whole crossing, not one per region");
        assertEquals(this.plugin.getMessages().formatRaw("Arena", "player", "Notch", "region", "shop"),
                this.player.titles.get(0)[0], "the highest-priority region's title must be the one shown");
    }

    @Test
    void displayChannelsFallThroughToTheNextRegionIndependently() {
        // the high-priority region has no action bar: the low-priority one still shows
        Region town = this.manager.createRegion(WORLD, "town", new CuboidShape(0, 0, 0, 10, 10, 10), 1, null);
        this.manager.setFlag(this.region, Flags.TITLE, GroupTarget.ALL, "Arena");
        this.manager.setFlag(town, Flags.ACTION_BAR, GroupTarget.ALL, "in town");

        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));

        assertEquals(1, this.player.titles.size());
        assertEquals(1, this.player.actionBars.size(), "an unset channel falls through to the next region");
    }

    @Test
    void regionWithoutDisplaysShowsNothing() {
        assertTrue(this.tracker.handleMove(this.player, at(5, 5, 5), false));
        assertEquals(0, this.player.titles.size());
        assertEquals(0, this.player.actionBars.size());
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
        private final List<Component> actionBars = new ArrayList<>();
        private final List<Component[]> titles = new ArrayList<>();
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
        public void sendActionBar(Component message) {
            this.actionBars.add(message);
        }

        @Override
        public void sendTitle(Component title, Component subtitle) {
            this.titles.add(new Component[]{title, subtitle});
        }

        @Override
        public void teleport(RegionLocation location) {
            this.location = location;
        }

        @Override
        public java.util.Optional<RegionLocation> findSafeSpot(RegionLocation target) {
            return java.util.Optional.of(target);
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

        private String gameMode = "survival";
        private Long playerTime = null;
        private Boolean playerWeather = null;
        private float walkSpeed = 0.2f;
        private float flySpeed = 0.1f;

        @Override
        public void setPlayerTime(long ticks) {
            this.playerTime = ticks;
        }

        @Override
        public void resetPlayerTime() {
            this.playerTime = null;
        }

        @Override
        public void setPlayerWeather(boolean rain) {
            this.playerWeather = rain;
        }

        @Override
        public void resetPlayerWeather() {
            this.playerWeather = null;
        }

        @Override
        public void setWalkSpeed(float speed) {
            this.walkSpeed = speed;
        }

        @Override
        public void setFlySpeed(float speed) {
            this.flySpeed = speed;
        }

        @Override
        public String getGameMode() {
            return this.gameMode;
        }

        @Override
        public void setGameMode(String mode) {
            this.gameMode = mode;
        }

        private double health = 20.0;
        private int foodLevel = 20;
        private boolean glowing = false;

        @Override
        public double getHealth() {
            return this.health;
        }

        @Override
        public void setHealth(double health) {
            this.health = health;
        }

        @Override
        public double getMaxHealth() {
            return 20.0;
        }

        @Override
        public int getFoodLevel() {
            return this.foodLevel;
        }

        @Override
        public void setFoodLevel(int level) {
            this.foodLevel = level;
        }

        @Override
        public void setGlowing(boolean glowing) {
            this.glowing = glowing;
        }
    }
}
