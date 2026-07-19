package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.GroupTarget;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.hooks.zmenu.FlagMaterials;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * The paginated flag editor of the managed region. Placeholders: {@code %flag%
 * %value% %default%}; the shown value is the region's explicit ALL-target one
 * ({@code unset} otherwise). Left-clicking a state flag cycles unset → deny →
 * allow → unset, right-clicking removes the value directly; both re-render on
 * the same page. Value-typed flags (text, lists) point back to {@code /rg flag}.
 */
public final class RegionFlagsButton extends PaginateButton {

    private final ZMenuGuiService service;

    public RegionFlagsButton(ZMenuGuiService service) {
        this.service = service;
    }

    private List<Flag<?>> flags() {
        return this.service.getPlugin().getFlagRegistry().getFlags().stream()
                .sorted(Comparator.comparing(Flag::getKey))
                .toList();
    }

    @Override
    public void onRender(Player player, InventoryEngine inventoryEngine) {
        Region region = this.service.managedRegion(player).orElse(null);
        if (region == null) {
            // render context: a direct close would be undone by zMenu's pending open
            this.service.handleRegionGoneRender(player);
            return;
        }
        paginate(flags(), inventoryEngine, (slot, flag) -> {
            Placeholders placeholders = new Placeholders();
            placeholders.register("flag", flag.getKey());
            placeholders.register("value", explicitValue(region, flag).orElse("unset"));
            placeholders.register("default", serializeDefault(flag));
            // localized one-line description, same source as /rg flags (flags.<key> in messages.yml)
            placeholders.register("description", this.service.getPlugin().getMessages().flagDescription(flag.getKey()));
            // a telling icon per flag; unknown flags keep the YAML template material
            ItemStack item = getItemStack().build(player, false, placeholders);
            Material material = FlagMaterials.resolve(flag.getKey());
            if (material != null) {
                item.setType(material);
            }
            inventoryEngine.addItem(slot, item)
                    .setClick(event -> onFlagClick(player, event, flag, inventoryEngine.getPage()));
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.service.getPlugin().getFlagRegistry().getFlags().size();
    }

    private void onFlagClick(Player player, InventoryClickEvent event, Flag<?> flag, int page) {
        Region live = this.service.managedRegion(player).orElse(null);
        if (live == null) {
            this.service.handleRegionGone(player);
            return;
        }
        if (!(flag.getDefaultValue() instanceof Boolean)) {
            this.service.getPlugin().getMessages().send(this.service.wrap(player), Message.GUI_FLAG_COMMAND_ONLY,
                    "flag", flag.getKey(),
                    "region", live.getName());
            return;
        }
        @SuppressWarnings("unchecked")
        Flag<Boolean> stateFlag = (Flag<Boolean>) flag;

        Optional<Boolean> current = live.getFlag(stateFlag, GroupTarget.ALL);
        if (event.isRightClick() || (current.isPresent() && current.get())) {
            // right click removes directly; a left click on "allow" completes the cycle
            this.service.getPlugin().getRegionManager().removeFlag(live, stateFlag, GroupTarget.ALL);
        } else if (current.isEmpty()) {
            this.service.getPlugin().getRegionManager().setFlag(live, stateFlag, GroupTarget.ALL, false);
        } else {
            this.service.getPlugin().getRegionManager().setFlag(live, stateFlag, GroupTarget.ALL, true);
        }
        this.service.openFlagEditor(player, live, page);
    }

    private <T> Optional<String> explicitValue(Region region, Flag<T> flag) {
        return region.getFlag(flag, GroupTarget.ALL).map(flag::serialize);
    }

    private static <T> String serializeDefault(Flag<T> flag) {
        return flag.serialize(flag.getDefaultValue());
    }
}
