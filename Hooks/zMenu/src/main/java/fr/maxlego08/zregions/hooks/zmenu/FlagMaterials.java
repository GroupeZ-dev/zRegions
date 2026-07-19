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

        // batch B1 — interactions & entities
        BY_FLAG.put("ride", Material.SADDLE);
        BY_FLAG.put("sleep", Material.RED_BED);
        BY_FLAG.put("respawn-anchor", Material.RESPAWN_ANCHOR);
        BY_FLAG.put("item-frame-rotation", Material.GLOW_ITEM_FRAME);
        BY_FLAG.put("use-anvil", Material.ANVIL);
        BY_FLAG.put("beacon", Material.BEACON);
        BY_FLAG.put("villager-trade", Material.EMERALD);
        BY_FLAG.put("shear", Material.SHEARS);
        BY_FLAG.put("leash", Material.LEAD);
        BY_FLAG.put("animal-breeding", Material.WHEAT);
        BY_FLAG.put("sign-edit", Material.DARK_OAK_SIGN);
        BY_FLAG.put("fishing-hook", Material.FISHING_ROD);
        BY_FLAG.put("projectile-launch", Material.BOW);
        BY_FLAG.put("receive-chat", Material.WRITABLE_BOOK);
        BY_FLAG.put("command-whitelist", Material.KNOWLEDGE_BOOK);

        // batch B2 — custom deny messages & exit displays
        BY_FLAG.put("deny-message", Material.BARRIER);
        BY_FLAG.put("entry-deny-message", Material.OAK_HANGING_SIGN);
        BY_FLAG.put("exit-deny-message", Material.SPRUCE_HANGING_SIGN);
        BY_FLAG.put("farewell-title", Material.NAME_TAG);
        BY_FLAG.put("farewell-subtitle", Material.BOOK);

        // batch B3 — environment / world control
        BY_FLAG.put("lightning", Material.LIGHTNING_ROD);
        BY_FLAG.put("lava-fire", Material.MAGMA_BLOCK);
        BY_FLAG.put("water-flow", Material.WATER_BUCKET);
        BY_FLAG.put("lava-flow", Material.LAVA_BUCKET);
        BY_FLAG.put("fire-burn", Material.CAMPFIRE);
        BY_FLAG.put("block-spread", Material.VINE);
        BY_FLAG.put("snow-fall", Material.SNOWBALL);
        BY_FLAG.put("snow-melt", Material.SNOW_BLOCK);
        BY_FLAG.put("ice-form", Material.ICE);
        BY_FLAG.put("ice-melt", Material.PACKED_ICE);
        BY_FLAG.put("frosted-ice-form", Material.FROSTED_ICE);
        BY_FLAG.put("frosted-ice-melt", Material.BLUE_ICE);
        BY_FLAG.put("soil-dry", Material.FARMLAND);
        BY_FLAG.put("coral-fade", Material.TUBE_CORAL);
        BY_FLAG.put("snowman-trails", Material.CARVED_PUMPKIN);

        // batch B4 — growth
        BY_FLAG.put("crop-growth", Material.WHEAT_SEEDS);
        BY_FLAG.put("tree-growth", Material.OAK_SAPLING);
        BY_FLAG.put("mushroom-growth", Material.RED_MUSHROOM);
        BY_FLAG.put("vine-growth", Material.WEEPING_VINES);
        BY_FLAG.put("grass-spread", Material.MOSS_BLOCK);
        BY_FLAG.put("mycelium-spread", Material.MYCELIUM);
        BY_FLAG.put("sculk-growth", Material.SCULK);
        BY_FLAG.put("bone-meal", Material.BONE_MEAL);
        BY_FLAG.put("entity-transform", Material.ZOMBIE_VILLAGER_SPAWN_EGG);

        // batch B5 — fine spawns
        BY_FLAG.put("animal-spawning", Material.PIG_SPAWN_EGG);
        BY_FLAG.put("monster-spawning", Material.SKELETON_SPAWN_EGG);
        BY_FLAG.put("spawner-spawning", Material.SPAWNER);
        BY_FLAG.put("phantom-spawning", Material.PHANTOM_SPAWN_EGG);
        BY_FLAG.put("slime-spawning", Material.SLIME_SPAWN_EGG);
        BY_FLAG.put("natural-spawning", Material.CREEPER_SPAWN_EGG);
        BY_FLAG.put("egg-spawning", Material.EGG);
        BY_FLAG.put("command-spawning", Material.REPEATING_COMMAND_BLOCK);
        BY_FLAG.put("raid-spawning", Material.CROSSBOW);
        BY_FLAG.put("patrol-spawning", Material.IRON_AXE);
        BY_FLAG.put("portal-spawning", Material.OBSIDIAN);
        BY_FLAG.put("deny-spawn", Material.STRUCTURE_VOID);

        // batch B6 — fine damage & explosions
        BY_FLAG.put("villager-damage", Material.VILLAGER_SPAWN_EGG);
        BY_FLAG.put("monster-damage", Material.IRON_SWORD);
        BY_FLAG.put("pet-damage", Material.BONE);
        BY_FLAG.put("firework-damage", Material.FIREWORK_STAR);
        BY_FLAG.put("entity-explosion-damage", Material.TNT_MINECART);
        BY_FLAG.put("creeper-explosion", Material.GUNPOWDER);
        BY_FLAG.put("tnt", Material.TNT);
        BY_FLAG.put("ghast-fireball", Material.GHAST_TEAR);
        BY_FLAG.put("wither-damage", Material.WITHER_SKELETON_SKULL);
        BY_FLAG.put("enderdragon-block-damage", Material.DRAGON_HEAD);
        BY_FLAG.put("potion-splash", Material.SPLASH_POTION);
        BY_FLAG.put("melee-pvp", Material.NETHERITE_SWORD);
        BY_FLAG.put("projectile-pvp", Material.ARROW);
    }

    private FlagMaterials() {
    }

    /** The icon for a flag key, or {@code null} to keep the YAML template material. */
    public static Material resolve(String flagKey) {
        return BY_FLAG.get(flagKey);
    }
}
