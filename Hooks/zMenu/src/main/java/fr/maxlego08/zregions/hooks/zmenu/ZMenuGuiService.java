package fr.maxlego08.zregions.hooks.zmenu;

import fr.maxlego08.menu.api.ButtonManager;
import fr.maxlego08.menu.api.Inventory;
import fr.maxlego08.menu.api.InventoryManager;
import fr.maxlego08.menu.api.exceptions.InventoryException;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.gui.GuiService;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.common.platform.RegionPlayerFactory;
import fr.maxlego08.zregions.common.plugin.ZRegionsPlugin;
import fr.maxlego08.zregions.hooks.zmenu.button.AddMemberButton;
import fr.maxlego08.zregions.hooks.zmenu.button.MembersButton;
import fr.maxlego08.zregions.hooks.zmenu.button.OpenMenuButton;
import fr.maxlego08.zregions.hooks.zmenu.button.PriorityButton;
import fr.maxlego08.zregions.hooks.zmenu.button.RegionFlagsButton;
import fr.maxlego08.zregions.hooks.zmenu.button.RegionInfoButton;
import fr.maxlego08.zregions.hooks.zmenu.button.RegionListButton;
import fr.maxlego08.zregions.hooks.zmenu.button.ShowBordersButton;
import fr.maxlego08.zregions.hooks.zmenu.loader.ServiceButtonLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * The zMenu-backed {@link GuiService}: five YAML inventories (bundled in the
 * jar, extracted to {@code plugins/zRegions/inventories/} on first use, fully
 * server-customizable) driven by zRegions button types. Instantiated by
 * reflection from {@code setupPlatformHooks()} only when zMenu is present and
 * enabled — this class must never be linked before that check.
 *
 * <p>Per-player state is the id of the region being managed; the live region is
 * re-resolved before every open and on every render, so a region deleted or
 * copy-swapped while its menu is open never goes stale. zMenu's registries are
 * not thread-safe: every interaction with them (load, reload, open) happens on
 * the game thread.</p>
 */
public final class ZMenuGuiService implements GuiService {

    private static final String REGIONS_FILE = "regions.yml";
    private static final String REGION_FILE = "region.yml";
    private static final String FLAGS_FILE = "flags.yml";
    private static final String MEMBERS_FILE = "members.yml";
    private static final String ADD_MEMBER_FILE = "add-member.yml";

    private final ZRegionsPlugin plugin;
    private final JavaPlugin loader;
    private final RegionPlayerFactory<Player> playerFactory;
    private final InventoryManager inventoryManager;
    private final Map<UUID, UUID> managedRegions = new ConcurrentHashMap<>();

    private Inventory regionsInventory;
    private Inventory regionInventory;
    private Inventory flagsInventory;
    private Inventory membersInventory;
    private Inventory addMemberInventory;

    public ZMenuGuiService(ZRegionsPlugin plugin, JavaPlugin loader, RegionPlayerFactory<Player> playerFactory)
            throws InventoryException {
        this.plugin = plugin;
        this.loader = loader;
        this.playerFactory = playerFactory;
        this.inventoryManager = provider(InventoryManager.class);

        ButtonManager buttonManager = provider(ButtonManager.class);
        // clear a previous instance's registrations (plugin-manager style re-enables):
        // zMenu appends per plugin without dedupe, both sides must be reset
        buttonManager.unregisters(loader);
        this.inventoryManager.deleteInventories(loader);
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_REGIONS", () -> new RegionListButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_REGION_INFO", () -> new RegionInfoButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_FLAGS", () -> new RegionFlagsButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_MEMBERS", () -> new MembersButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_ADD_MEMBER", () -> new AddMemberButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_OPEN_FLAGS", () -> new OpenMenuButton(this, OpenMenuButton.Target.FLAGS)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_OPEN_REGION", () -> new OpenMenuButton(this, OpenMenuButton.Target.REGION)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_OPEN_LIST", () -> new OpenMenuButton(this, OpenMenuButton.Target.LIST)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_OPEN_MEMBERS", () -> new OpenMenuButton(this, OpenMenuButton.Target.MEMBERS)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_OPEN_ADD_MEMBER", () -> new OpenMenuButton(this, OpenMenuButton.Target.ADD_MEMBER)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_SHOW_BORDERS", () -> new ShowBordersButton(this)));
        buttonManager.register(new ServiceButtonLoader(loader, "ZREGIONS_PRIORITY", () -> new PriorityButton(this)));

        loadInventories();
    }

    private <T> T provider(Class<T> type) {
        RegisteredServiceProvider<T> provider = Bukkit.getServicesManager().getRegistration(type);
        if (provider == null) {
            throw new IllegalStateException("zMenu did not register " + type.getSimpleName());
        }
        return provider.getProvider();
    }

    private void loadInventories() throws InventoryException {
        this.regionsInventory = loadOrWarn(REGIONS_FILE);
        this.regionInventory = loadOrWarn(REGION_FILE);
        this.flagsInventory = loadOrWarn(FLAGS_FILE);
        this.membersInventory = loadOrWarn(MEMBERS_FILE);
        this.addMemberInventory = loadOrWarn(ADD_MEMBER_FILE);
    }

    /**
     * Extracts the inventory in the language.yml language on first use
     * ({@code languages/<lang>/inventories/<file>} in the jar, English fallback)
     * to {@code plugins/zRegions/inventories/<file>}, then loads the DISK file —
     * the bundled translations only ever provide the defaults, like messages.yml.
     * zMenu returns null (no exception) for an inventory disabled by its YAML.
     */
    private Inventory loadOrWarn(String fileName) throws InventoryException {
        Path file = this.plugin.getBootstrap().getDataDirectory().resolve("inventories").resolve(fileName);
        if (!Files.exists(file)) {
            String jarPath = "languages/" + this.plugin.getLanguage() + "/inventories/" + fileName;
            InputStream input = this.plugin.getBootstrap().getResourceStream(jarPath);
            if (input == null) {
                input = this.plugin.getBootstrap().getResourceStream("languages/en/inventories/" + fileName);
            }
            if (input == null) {
                this.plugin.getLogger().warn("No bundled " + jarPath
                        + " in the jar — the matching zRegions menu is disabled.");
                return null;
            }
            try (InputStream in = input) {
                Files.createDirectories(file.getParent());
                Files.copy(in, file);
            } catch (IOException exception) {
                this.plugin.getLogger().warn("Unable to extract " + jarPath, exception);
                return null;
            }
        }

        Inventory inventory = this.inventoryManager.loadInventory(this.loader, file.toFile());
        if (inventory == null) {
            this.plugin.getLogger().warn("zMenu did not load '" + fileName
                    + "' (enable: false?) — the matching zRegions menu is disabled.");
        }
        return inventory;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void openRegionList(RegionPlayer player) {
        open(player.getUniqueId(), this.regionsInventory, 1, false);
    }

    @Override
    public void openRegionMenu(RegionPlayer player, Region region) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.regionInventory, 1, true);
    }

    @Override
    public void openFlagEditor(RegionPlayer player, Region region) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.flagsInventory, 1, true);
    }

    @Override
    public void openMemberManager(RegionPlayer player, Region region) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.membersInventory, 1, true);
    }

    /** zMenu's registries are main-thread only; /rg reload runs on the async pool. */
    @Override
    public void reload() {
        this.plugin.getBootstrap().getScheduler().executeSync(() -> {
            this.inventoryManager.deleteInventories(this.loader);
            try {
                loadInventories();
            } catch (InventoryException exception) {
                this.plugin.getLogger().warn("Unable to reload the zMenu inventories, the GUI may be broken until restart.",
                        exception);
            }
        });
    }

    /**
     * Commands run on the async pool and clicks re-open follow-up menus: both
     * funnel here, and the actual open always happens on the game thread. Menus
     * needing a managed region validate it at open time — a vanished region
     * refuses the open (the previous menu simply stays) instead of showing a
     * dead menu, which a close-during-render could not prevent (zMenu always
     * finishes its scheduled open).
     */
    private void open(UUID playerId, Inventory inventory, int page, boolean requiresRegion) {
        if (inventory == null) {
            return;
        }
        this.plugin.getBootstrap().getScheduler().executeSync(() -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player == null) {
                return;
            }
            if (requiresRegion && managedRegion(player).isEmpty()) {
                this.plugin.getMessages().send(wrap(player), Message.GUI_REGION_GONE);
                return;
            }
            this.inventoryManager.openInventory(player, inventory, page);
        });
    }

    // --- shared button helpers (the buttons live in the button/ sub-package) ---

    public ZRegionsPlugin getPlugin() {
        return this.plugin;
    }

    /** The live region the player is managing — empty once deleted. */
    public Optional<Region> managedRegion(Player player) {
        UUID regionId = this.managedRegions.get(player.getUniqueId());
        return regionId == null ? Optional.empty() : this.plugin.getRegionManager().getRegion(regionId);
    }

    public RegionPlayer wrap(Player player) {
        return this.playerFactory.wrap(player);
    }

    public void openRegionMenu(Player player, Region region) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.regionInventory, 1, true);
    }

    public void openRegionList(Player player) {
        open(player.getUniqueId(), this.regionsInventory, 1, false);
    }

    public void openFlagEditor(Player player, Region region, int page) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.flagsInventory, page, true);
    }

    public void openMemberManager(Player player, Region region, int page) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.membersInventory, page, true);
    }

    public void openAddMember(Player player, Region region) {
        this.managedRegions.put(player.getUniqueId(), region.getId());
        open(player.getUniqueId(), this.addMemberInventory, 1, true);
    }

    /**
     * The managed region vanished, seen from a CLICK: closing right away is safe
     * there (no pending zMenu open).
     */
    public void handleRegionGone(Player player) {
        player.closeInventory();
        this.plugin.getMessages().send(wrap(player), Message.GUI_REGION_GONE);
    }

    /**
     * The managed region vanished, seen from a RENDER (zMenu page turns re-open
     * through its own pipeline, bypassing {@link #open}): a close during render
     * would be undone by zMenu's already-scheduled open, so the close is delayed
     * past it.
     */
    public void handleRegionGoneRender(Player player) {
        UUID playerId = player.getUniqueId();
        this.plugin.getBootstrap().getScheduler().asyncLater(() ->
                this.plugin.getBootstrap().getScheduler().executeSync(() -> {
                    Player online = Bukkit.getPlayer(playerId);
                    if (online != null) {
                        online.closeInventory();
                        this.plugin.getMessages().send(wrap(online), Message.GUI_REGION_GONE);
                    }
                }), 100, TimeUnit.MILLISECONDS);
    }
}
