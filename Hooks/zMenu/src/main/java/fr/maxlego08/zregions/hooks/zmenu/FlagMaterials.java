package fr.maxlego08.zregions.hooks.zmenu;

import org.bukkit.Material;

import java.util.HashMap;
import java.util.Map;

/**
 * Default icon per built-in flag for the zMenu flag editor: instead of a uniform
 * NAME_TAG, each flag shows a telling item (pvp → diamond sword, block-break →
 * iron pickaxe…). Only the icon changes; the YAML item still drives the name and
 * lore. Unknown flags (addons, or a key missing here) keep the template material.
 *
 * <p>Every value is a {@link Material} constant, so the compiler (which targets
 * spigot-api 1.20.4, the supported floor) guarantees the icons exist on every
 * supported server — no cross-version surprise.</p>
 */
public final class FlagMaterials {

    private static final Map<String, Material> BY_FLAG = new HashMap<>();

    static {
        // blocks (player-driven)
        BY_FLAG.put("block-break", Material.IRON_PICKAXE);
        BY_FLAG.put("block-place", Material.GRASS_BLOCK);
        BY_FLAG.put("interact", Material.OAK_BUTTON);
        BY_FLAG.put("container-access", Material.CHEST);
        BY_FLAG.put("bucket-fill", Material.BUCKET);
        BY_FLAG.put("bucket-empty", Material.WATER_BUCKET);
        BY_FLAG.put("armor-stand", Material.ARMOR_STAND);
        BY_FLAG.put("hanging-break", Material.PAINTING);
        BY_FLAG.put("hanging-place", Material.ITEM_FRAME);
        BY_FLAG.put("vehicle-place", Material.OAK_BOAT);
        BY_FLAG.put("vehicle-destroy", Material.MINECART);
        BY_FLAG.put("crop-trample", Material.FARMLAND);

        // blocks (environment)
        BY_FLAG.put("redstone", Material.REDSTONE);
        BY_FLAG.put("piston", Material.PISTON);
        BY_FLAG.put("fire-ignite", Material.FLINT_AND_STEEL);
        BY_FLAG.put("fire-spread", Material.FIRE_CHARGE);
        BY_FLAG.put("fluid-flow", Material.LAVA_BUCKET);
        BY_FLAG.put("leaf-decay", Material.OAK_LEAVES);
        BY_FLAG.put("block-explosion", Material.TNT);
        BY_FLAG.put("entity-explosion", Material.CREEPER_HEAD);

        // entities
        BY_FLAG.put("mob-spawning", Material.SPAWNER);
        BY_FLAG.put("mob-griefing", Material.ENDERMAN_SPAWN_EGG);
        BY_FLAG.put("damage-animals", Material.COW_SPAWN_EGG);
        BY_FLAG.put("mob-damage", Material.ZOMBIE_SPAWN_EGG);

        // players
        BY_FLAG.put("pvp", Material.DIAMOND_SWORD);
        BY_FLAG.put("invincible", Material.ENCHANTED_GOLDEN_APPLE);
        BY_FLAG.put("fall-damage", Material.FEATHER);
        BY_FLAG.put("hunger", Material.COOKED_BEEF);
        BY_FLAG.put("item-drop", Material.DROPPER);
        BY_FLAG.put("item-pickup", Material.HOPPER);
        BY_FLAG.put("enderpearl", Material.ENDER_PEARL);
        BY_FLAG.put("chorus-fruit", Material.CHORUS_FRUIT);
        BY_FLAG.put("keep-inventory", Material.ENDER_CHEST);
        BY_FLAG.put("exp-drop", Material.EXPERIENCE_BOTTLE);
        BY_FLAG.put("chat", Material.PAPER);
        BY_FLAG.put("elytra", Material.ELYTRA);
        BY_FLAG.put("fly", Material.FIREWORK_ROCKET);
        BY_FLAG.put("totem", Material.TOTEM_OF_UNDYING);
        BY_FLAG.put("command-blacklist", Material.COMMAND_BLOCK);

        // zone
        BY_FLAG.put("entry", Material.OAK_DOOR);
        BY_FLAG.put("exit", Material.IRON_DOOR);
        BY_FLAG.put("greeting", Material.OAK_SIGN);
        BY_FLAG.put("farewell", Material.SPRUCE_SIGN);
        BY_FLAG.put("title", Material.NAME_TAG);
        BY_FLAG.put("subtitle", Material.BOOK);
        BY_FLAG.put("action-bar", Material.MAP);
    }

    private FlagMaterials() {
    }

    /** The icon for a flag key, or {@code null} to keep the YAML template material. */
    public static Material resolve(String flagKey) {
        return BY_FLAG.get(flagKey);
    }
}
