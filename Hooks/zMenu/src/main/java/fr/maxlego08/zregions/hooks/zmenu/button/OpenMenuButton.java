package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * Navigation buttons between the zRegions menus. LIST needs no context; the
 * other targets resolve the player's managed region and explain when it is
 * gone instead of showing a dead menu.
 */
public final class OpenMenuButton extends Button {

    public enum Target {LIST, REGION, FLAGS, MEMBERS, ADD_MEMBER}

    private final ZMenuGuiService service;
    private final Target target;

    public OpenMenuButton(ZMenuGuiService service, Target target) {
        this.service = service;
        this.target = target;
    }

    @Override
    public void onClick(Player player, InventoryClickEvent event, InventoryEngine inventory, int slot, Placeholders placeholders) {
        super.onClick(player, event, inventory, slot, placeholders);

        if (this.target == Target.LIST) {
            this.service.openRegionList(player);
            return;
        }
        Region region = this.service.managedRegion(player).orElse(null);
        if (region == null) {
            this.service.handleRegionGone(player);
            return;
        }
        switch (this.target) {
            case REGION -> this.service.openRegionMenu(player, region);
            case FLAGS -> this.service.openFlagEditor(player, region, 1);
            case MEMBERS -> this.service.openMemberManager(player, region, 1);
            case ADD_MEMBER -> this.service.openAddMember(player, region);
            default -> throw new IllegalStateException("Unhandled target " + this.target);
        }
    }
}
