package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.Button;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;

/**
 * The managed region's details, rendered into the YAML item's placeholders:
 * {@code %region% %world% %shape% %priority% %members% %parent%}.
 */
public final class RegionInfoButton extends Button {

    private final ZMenuGuiService service;

    public RegionInfoButton(ZMenuGuiService service) {
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
        placeholders.register("region", region.getName());
        placeholders.register("world", region.getWorldName());
        placeholders.register("shape", region.getShape() == null ? "GLOBAL" : region.getShape().getType().name());
        placeholders.register("priority", String.valueOf(region.getPriority()));
        placeholders.register("members", String.valueOf(region.getMembers().size()));
        placeholders.register("parent", region.getParentId()
                .flatMap(parentId -> this.service.getPlugin().getRegionManager().getRegion(parentId))
                .map(Region::getName).orElse("-"));
        inventoryEngine.addItem(getSlot(), getItemStack().build(player, false, placeholders));
    }
}
