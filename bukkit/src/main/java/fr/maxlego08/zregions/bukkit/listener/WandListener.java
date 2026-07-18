package fr.maxlego08.zregions.bukkit.listener;

import fr.maxlego08.zregions.bukkit.BukkitPlayerFactory;
import fr.maxlego08.zregions.bukkit.ZRegionsBukkitPlugin;
import fr.maxlego08.zregions.common.locale.Message;
import fr.maxlego08.zregions.common.platform.RegionLocation;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Turns wand clicks into selection corners: left click = pos1, right click =
 * pos2, on the clicked block. The wand is identified by its PDC marker
 * ({@link BukkitPlayerFactory#WAND_KEY}) so renaming or moving the item never
 * breaks it. Runs at LOWEST priority and cancels the event, so the protection
 * listeners (ignoreCancelled) never deny a selection click inside a foreign
 * region.
 */
public final class WandListener implements Listener {

    private static final String ADMIN_PERMISSION = "zregions.admin";

    private final ZRegionsBukkitPlugin plugin;

    public WandListener(ZRegionsBukkitPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        Block block = event.getClickedBlock();
        if (block == null) return;
        ItemStack item = event.getItem();
        if (item == null || !isWand(item)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission(ADMIN_PERMISSION)) return;

        Action action = event.getAction();
        RegionLocation location = new RegionLocation(block.getWorld().getName(),
                block.getX(), block.getY(), block.getZ());
        if (action == Action.LEFT_CLICK_BLOCK) {
            this.plugin.getSelectionManager().setPos1(player.getUniqueId(), location);
            send(player, Message.POS1_SET, location);
        } else if (action == Action.RIGHT_CLICK_BLOCK) {
            this.plugin.getSelectionManager().setPos2(player.getUniqueId(), location);
            send(player, Message.POS2_SET, location);
        } else {
            return;
        }
        event.setCancelled(true);
    }

    private static boolean isWand(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer()
                .has(BukkitPlayerFactory.WAND_KEY, PersistentDataType.BYTE);
    }

    private void send(Player player, Message message, RegionLocation location) {
        this.plugin.getMessages().send(this.plugin.getPlayerFactory().wrap(player),
                message, "position", location.toString());
    }
}
