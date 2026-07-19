package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionPlayer;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Outlines the managed region's borders with the per-player particle display
 * (config-default duration), then closes the menu so the outline is visible.
 * The global region has no shape to outline.
 */
public final class ShowBordersButton extends Button {

    private final ZMenuGuiService service;

    public ShowBordersButton(ZMenuGuiService service) {
        this.service = service;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        super.onClick(player, event, inventory, slot, placeholders);

        Region region = this.service.managedRegion(player).orElse(null);
        if (region == null) {
            this.service.handleRegionGone(player);
            return;
        }
        RegionPlayer wrapped = this.service.wrap(player);
        if (region.isGlobal()) {
            this.service.getPlugin().getMessages().send(wrapped, Message.BORDER_GLOBAL);
            return;
        }
        int seconds = this.service.getPlugin().getBorderDisplay().show(wrapped, region, 0);
        player.closeInventory();
        this.service.getPlugin().getMessages().send(wrapped, Message.BORDER_SHOWN,
                "region", region.getName(),
                "seconds", String.valueOf(seconds));
    }
}
