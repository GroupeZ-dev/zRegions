package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * The paginated list of online players who are not yet members of the managed
 * region. Placeholder: {@code %player%}. Clicking adds the player as a member
 * and returns to the member manager (offline players go through
 * {@code /rg addmember}).
 */
public final class AddMemberButton extends PaginateButton {

    private final ZMenuGuiService service;

    public AddMemberButton(ZMenuGuiService service) {
        this.service = service;
    }

    private List<Player> candidates(Region region) {
        return Bukkit.getOnlinePlayers().stream()
                .filter(online -> region.getMembers().stream()
                        .noneMatch(member -> member.getPlayerId().equals(online.getUniqueId())))
                .sorted(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER))
                .map(online -> (Player) online)
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
        paginate(candidates(region), inventoryEngine, (slot, candidate) -> {
            Placeholders placeholders = new Placeholders();
            placeholders.register("player", candidate.getName());
            inventoryEngine.addItem(slot, getItemStack().build(player, false, placeholders))
                    .setClick(event -> onCandidateClick(player, candidate));
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.service.managedRegion(player).map(region -> candidates(region).size()).orElse(0);
    }

    private void onCandidateClick(Player player, Player candidate) {
        Region live = this.service.managedRegion(player).orElse(null);
        if (live == null) {
            this.service.handleRegionGone(player);
            return;
        }
        // the candidate list may be stale: never overwrite a role set meanwhile
        boolean alreadyMember = live.getMembers().stream()
                .anyMatch(member -> member.getPlayerId().equals(candidate.getUniqueId()));
        if (alreadyMember) {
            this.service.openMemberManager(player, live, 1);
            return;
        }
        this.service.getPlugin().getRegionManager().setMember(live, candidate.getUniqueId(), MemberRole.MEMBER);
        this.service.getPlugin().getMessages().send(this.service.wrap(player), Message.MEMBER_ADDED,
                "player", candidate.getName(),
                "role", MemberRole.MEMBER.name().toLowerCase(Locale.ROOT),
                "region", live.getName());
        this.service.openMemberManager(player, live, 1);
    }
}
