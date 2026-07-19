package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

/**
 * A +/- priority control inside a region's menu: left click +1, right click −1,
 * shift ×10. The current priority is shown live through the {@code %priority%}
 * placeholder and the menu re-renders after each change. Priority never drops
 * below 0. The global region keeps its priority untouched (it applies as a
 * world-wide fallback regardless of priority).
 */
public final class PriorityButton extends Button {

    private final ZMenuGuiService service;

    public PriorityButton(ZMenuGuiService service) {
        this.service = service;
    }

    @Override
    public boolean hasSpecialRender() {
        return true;
    }

    @Override
    public void onRender(Player player, InventoryEngine inventoryEngine) {
        Region region = this.service.managedRegion(player).orElse(null);
        if (region == null) {
            // render context: a direct close would be undone by zMenu's pending open
            this.service.handleRegionGoneRender(player);
            return;
        }
        Placeholders placeholders = new Placeholders();
        placeholders.register("priority", String.valueOf(region.getPriority()));
        inventoryEngine.addItem(getSlot(), getItemStack().build(player, false, placeholders))
                .setClick(event -> onPriorityClick(player, event));
    }

    private void onPriorityClick(Player player, InventoryClickEvent event) {
        Region live = this.service.managedRegion(player).orElse(null);
        if (live == null) {
            this.service.handleRegionGone(player);
            return;
        }
        // the global region's priority is meaningless (applied as fallback regardless)
        if (!live.isGlobal()) {
            int step = event.isShiftClick() ? 10 : 1;
            int delta = event.isRightClick() ? -step : step;
            int next = Math.max(0, live.getPriority() + delta);
            if (next != live.getPriority()) {
                this.service.getPlugin().getRegionManager().setPriority(live, next);
            }
        }
        // re-render the region menu so %priority% (and the info item) refresh
        this.service.openRegionMenu(player, live);
    }
}
