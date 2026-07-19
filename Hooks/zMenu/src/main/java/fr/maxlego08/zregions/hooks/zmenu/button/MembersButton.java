package fr.maxlego08.zregions.hooks.zmenu.button;

import fr.maxlego08.menu.api.button.PaginateButton;
import fr.maxlego08.menu.api.engine.InventoryEngine;
import fr.maxlego08.menu.api.utils.Placeholders;
import fr.maxlego08.zregions.api.region.MemberRole;
import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.region.RegionMember;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.hooks.zmenu.ZMenuGuiService;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * The paginated member list of the managed region (owners first, stable UUID
 * order — sorting never touches username lookups: on Spigot an offline-player
 * name is a playerdata disk read, so names resolve once per RENDERED row only).
 * Placeholders: {@code %player% %role%}. Left click toggles the role owner ↔
 * member, right click removes; both re-check the LIVE membership first — a
 * member removed by someone else meanwhile is reported, never resurrected.
 */
public final class MembersButton extends PaginateButton {

    private final ZMenuGuiService service;

    public MembersButton(ZMenuGuiService service) {
        this.service = service;
    }

    /** Sorted without any username lookup (no I/O in a comparator). */
    private List<RegionMember> members(Region region) {
        return region.getMembers().stream()
                .sorted(Comparator.comparing((RegionMember member) -> member.getRole() != MemberRole.OWNER)
                        .thenComparing(RegionMember::getPlayerId))
                .toList();
    }

    private String memberName(UUID playerId) {
        return this.service.getPlugin().getBootstrap().lookupUsername(playerId)
                .orElseGet(() -> playerId.toString().substring(0, 8));
    }

    @Override
    public void onRender(Player player, InventoryEngine inventoryEngine) {
        Region region = this.service.managedRegion(player).orElse(null);
        if (region == null) {
            // render context: a direct close would be undone by zMenu's pending open
            this.service.handleRegionGoneRender(player);
            return;
        }
        paginate(members(region), inventoryEngine, (slot, member) -> {
            String name = memberName(member.getPlayerId());
            Placeholders placeholders = new Placeholders();
            placeholders.register("player", name);
            placeholders.register("role", member.getRole().name().toLowerCase(Locale.ROOT));
            inventoryEngine.addItem(slot, getItemStack().build(player, false, placeholders))
                    .setClick(event -> onMemberClick(player, event, member.getPlayerId(), name, inventoryEngine.getPage()));
        });
    }

    @Override
    public int getPaginationSize(Player player) {
        return this.service.managedRegion(player).map(region -> region.getMembers().size()).orElse(0);
    }

    private void onMemberClick(Player player, InventoryClickEvent event, UUID playerId, String name, int page) {
        Region live = this.service.managedRegion(player).orElse(null);
        if (live == null) {
            this.service.handleRegionGone(player);
            return;
        }
        // the render-time row may be stale: only the LIVE membership decides
        Optional<RegionMember> current = live.getMembers().stream()
                .filter(member -> member.getPlayerId().equals(playerId))
                .findFirst();
        if (current.isEmpty()) {
            this.service.getPlugin().getMessages().send(this.service.wrap(player), Message.MEMBER_NOT_MEMBER,
                    "player", name,
                    "region", live.getName());
            this.service.openMemberManager(player, live, clampPage(page, live.getMembers().size()));
            return;
        }

        if (event.isRightClick()) {
            this.service.getPlugin().getRegionManager().removeMember(live, playerId);
            this.service.getPlugin().getMessages().send(this.service.wrap(player), Message.MEMBER_REMOVED,
                    "player", name,
                    "region", live.getName());
        } else {
            MemberRole role = current.get().getRole() == MemberRole.OWNER ? MemberRole.MEMBER : MemberRole.OWNER;
            this.service.getPlugin().getRegionManager().setMember(live, playerId, role);
            this.service.getPlugin().getMessages().send(this.service.wrap(player), Message.MEMBER_ADDED,
                    "player", name,
                    "role", role.name().toLowerCase(Locale.ROOT),
                    "region", live.getName());
        }
        this.service.openMemberManager(player, live, clampPage(page, live.getMembers().size()));
    }

    /** zMenu never clamps pages: removing the last row of the last page must not land on an empty page. */
    private int clampPage(int page, int memberCount) {
        int pageSize = Math.max(1, getSlots().size());
        int maxPage = Math.max(1, (memberCount + pageSize - 1) / pageSize);
        return Math.min(page, maxPage);
    }
}
