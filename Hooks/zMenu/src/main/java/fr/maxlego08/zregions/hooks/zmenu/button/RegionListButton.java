package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;

/**
 * The paginated list of every region (world then name order). Placeholders:
 * {@code %region% %world% %shape% %priority% %members%}. Clicking opens the
 * region's management menu.
 */
public final class RegionListButton extends PaginateButton {

    private final ZMenuGuiService service;

    public RegionListButton(ZMenuGuiService service) {
        this.service = service;
    }

    private List<Region> regions() {
        return this.service.getPlugin().getRegionManager().getRegions().stream()
                .sorted(Comparator.comparing(Region::getWorldName)
                        .thenComparing(Region::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Override
    public void onRender(Player player, InventoryEngine inventoryEngine) {
        paginate(regions(), inventoryEngine, (slot, region) -> {
            Placeholders placeholders = new Placeholders();
            placeholders.register("region", region.getName());
            placeholders.register("world", region.getWorldName());
            placeholders.register("shape", region.getShape() == null ? "GLOBAL" : region.getShape().getType().name());
            placeholders.register("priority", String.valueOf(region.getPriority()));
            placeholders.register("members", String.valueOf(region.getMembers().size()));
            inventoryEngine.addItem(slot, getItemStack().build(player, false, placeholders))
                    .setClick(event -> this.service.openRegionMenu(player, region));
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.service.getPlugin().getRegionManager().getRegions().size();
    }
}
