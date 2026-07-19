package fr.maxlego08.zregions.common.gui;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.platform.RegionPlayer;

/**
 * Optional GUI backend (plan §13): the plugin is 100 % usable through commands,
 * a platform hook (Hooks/zMenu on Bukkit) may layer inventories on top. The
 * interface only speaks common types — every platform/zMenu detail stays in the
 * hook implementation. {@link #NONE} is the installed default; commands fall
 * back to text when {@link #isAvailable()} is false.
 */
public interface GuiService {

    /** The no-GUI default: everything unavailable, reload is a no-op. */
    GuiService NONE = new GuiService() {
        @Override
        public boolean isAvailable() {
            return false;
        }

        @Override
        public void openRegionList(RegionPlayer player) {
        }

        @Override
        public void openRegionMenu(RegionPlayer player, Region region) {
        }

        @Override
        public void openFlagEditor(RegionPlayer player, Region region) {
        }

        @Override
        public void openMemberManager(RegionPlayer player, Region region) {
        }
    };

    /** Whether a GUI backend is installed (zMenu present and hooked). */
    boolean isAvailable();

    /** Opens the paginated list of every region. */
    void openRegionList(RegionPlayer player);

    /** Opens the management menu of one region. */
    void openRegionMenu(RegionPlayer player, Region region);

    /** Opens the flag editor of one region. */
    void openFlagEditor(RegionPlayer player, Region region);

    /** Opens the member manager of one region (add, remove, change roles). */
    void openMemberManager(RegionPlayer player, Region region);

    /** Re-reads the inventory files ({@code /rg reload}). */
    default void reload() {
    }
}
