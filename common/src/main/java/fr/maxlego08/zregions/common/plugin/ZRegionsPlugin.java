package fr.maxlego08.zregions.common.plugin;

import fr.maxlego08.zregions.api.flag.FlagRegistry;
import fr.maxlego08.zregions.api.manager.RegionManager;
import fr.maxlego08.zregions.common.command.RegionCommandManager;
import fr.maxlego08.zregions.common.config.ZRegionsConfiguration;
import fr.maxlego08.zregions.common.gui.GuiService;
import fr.maxlego08.zregions.common.locale.MessageService;
import fr.maxlego08.zregions.common.movement.RegionMovementTracker;
import fr.maxlego08.zregions.common.plugin.bootstrap.ZRegionsBootstrap;
import fr.maxlego08.zregions.common.plugin.logging.PluginLogger;
import fr.maxlego08.zregions.common.selection.SelectionManager;
import fr.maxlego08.zregions.common.sender.RegionSender;
import fr.maxlego08.zregions.common.storage.RegionStorage;
import fr.maxlego08.zregions.common.visual.BorderDisplayManager;

/**
 * The internal plugin contract (LuckPerms' LuckPermsPlugin model). Every platform
 * implementation extends {@link AbstractZRegionsPlugin}; the getters below are what
 * the rest of the common module codes against.
 */
public interface ZRegionsPlugin {

    ZRegionsBootstrap getBootstrap();

    default PluginLogger getLogger() {
        return getBootstrap().getPluginLogger();
    }

    ZRegionsConfiguration getConfiguration();

    MessageService getMessages();

    RegionStorage getStorage();

    RegionManager getRegionManager();

    FlagRegistry getFlagRegistry();

    RegionCommandManager getCommandManager();

    SelectionManager getSelectionManager();

    RegionMovementTracker getMovementTracker();

    BorderDisplayManager getBorderDisplay();

    /** The optional GUI backend — {@link GuiService#NONE} unless a hook installed one. */
    default GuiService getGuiService() {
        return GuiService.NONE;
    }

    /**
     * The language of the bundled default files, resolved from language.yml
     * ({@code auto} → JVM locale, always one of the bundled languages).
     */
    default String getLanguage() {
        return "en";
    }

    /** Re-reads config.yml and the messages file of the (possibly changed) language. */
    void reload();

    /** A sender wrapping the platform console. */
    RegionSender getConsoleSender();
}
