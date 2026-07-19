package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.FlagRegistry;

import java.util.List;

/**
 * The built-in flags. State flags default to allow — a freshly created region
 * changes nothing until an owner denies something ({@code invincible} and
 * {@code keep-inventory} are the exceptions: those protections are off until a
 * region allows them). Registered once at startup, before regions are loaded
 * (stored values need the registry to parse).
 *
 * <p>Every flag listed here is enforced by a platform listener — no dead flags.</p>
 */
public final class Flags {

    // --- blocks (player-driven) ---
    public static final Flag<Boolean> BLOCK_BREAK = state("block-break");
    public static final Flag<Boolean> BLOCK_PLACE = state("block-place");
    public static final Flag<Boolean> INTERACT = state("interact");
    public static final Flag<Boolean> CONTAINER_ACCESS = state("container-access");
    public static final Flag<Boolean> BUCKET_FILL = state("bucket-fill");
    public static final Flag<Boolean> BUCKET_EMPTY = state("bucket-empty");
    public static final Flag<Boolean> ARMOR_STAND = state("armor-stand");
    public static final Flag<Boolean> HANGING_BREAK = state("hanging-break");
    public static final Flag<Boolean> HANGING_PLACE = state("hanging-place");
    public static final Flag<Boolean> VEHICLE_PLACE = state("vehicle-place");
    public static final Flag<Boolean> VEHICLE_DESTROY = state("vehicle-destroy");
    public static final Flag<Boolean> CROP_TRAMPLE = state("crop-trample");

    // --- blocks (environment) ---
    public static final Flag<Boolean> REDSTONE = state("redstone");
    public static final Flag<Boolean> PISTON = state("piston");
    public static final Flag<Boolean> FIRE_IGNITE = state("fire-ignite");
    public static final Flag<Boolean> FIRE_SPREAD = state("fire-spread");
    public static final Flag<Boolean> FLUID_FLOW = state("fluid-flow");
    public static final Flag<Boolean> LEAF_DECAY = state("leaf-decay");
    public static final Flag<Boolean> BLOCK_EXPLOSION = state("block-explosion");
    public static final Flag<Boolean> ENTITY_EXPLOSION = state("entity-explosion");

    // --- entities ---
    public static final Flag<Boolean> MOB_SPAWNING = state("mob-spawning");
    public static final Flag<Boolean> MOB_GRIEFING = state("mob-griefing");
    public static final Flag<Boolean> DAMAGE_ANIMALS = state("damage-animals");
    public static final Flag<Boolean> MOB_DAMAGE = state("mob-damage");

    // --- players ---
    public static final Flag<Boolean> PVP = state("pvp");
    public static final Flag<Boolean> INVINCIBLE = new StateFlag("invincible", false);
    public static final Flag<Boolean> FALL_DAMAGE = state("fall-damage");
    public static final Flag<Boolean> HUNGER = state("hunger");
    public static final Flag<Boolean> ITEM_DROP = state("item-drop");
    public static final Flag<Boolean> ITEM_PICKUP = state("item-pickup");
    public static final Flag<Boolean> ENDERPEARL = state("enderpearl");
    public static final Flag<Boolean> CHORUS_FRUIT = state("chorus-fruit");
    public static final Flag<Boolean> KEEP_INVENTORY = new StateFlag("keep-inventory", false);
    public static final Flag<Boolean> EXP_DROP = state("exp-drop");
    public static final Flag<Boolean> CHAT = state("chat");
    public static final Flag<Boolean> ELYTRA = state("elytra");
    public static final Flag<Boolean> FLY = state("fly");
    public static final Flag<Boolean> TOTEM = state("totem");
    public static final Flag<List<String>> COMMAND_BLACKLIST = new StringListFlag("command-blacklist", List.of());

    // --- interactions & entities (batch B1) ---
    public static final Flag<Boolean> RIDE = state("ride");
    public static final Flag<Boolean> SLEEP = state("sleep");
    public static final Flag<Boolean> RESPAWN_ANCHOR = state("respawn-anchor");
    public static final Flag<Boolean> ITEM_FRAME_ROTATION = state("item-frame-rotation");
    public static final Flag<Boolean> USE_ANVIL = state("use-anvil");
    public static final Flag<Boolean> BEACON = state("beacon");
    public static final Flag<Boolean> VILLAGER_TRADE = state("villager-trade");
    public static final Flag<Boolean> SHEAR = state("shear");
    public static final Flag<Boolean> LEASH = state("leash");
    public static final Flag<Boolean> ANIMAL_BREEDING = state("animal-breeding");
    public static final Flag<Boolean> SIGN_EDIT = state("sign-edit");
    public static final Flag<Boolean> FISHING_HOOK = state("fishing-hook");
    public static final Flag<Boolean> PROJECTILE_LAUNCH = state("projectile-launch");
    public static final Flag<Boolean> RECEIVE_CHAT = state("receive-chat");
    public static final Flag<List<String>> COMMAND_WHITELIST = new StringListFlag("command-whitelist", List.of());

    // --- custom deny messages & exit displays (batch B2) ---
    public static final Flag<String> DENY_MESSAGE = new StringFlag("deny-message", "");
    public static final Flag<String> ENTRY_DENY_MESSAGE = new StringFlag("entry-deny-message", "");
    public static final Flag<String> EXIT_DENY_MESSAGE = new StringFlag("exit-deny-message", "");
    public static final Flag<String> FAREWELL_TITLE = new StringFlag("farewell-title", "");
    public static final Flag<String> FAREWELL_SUBTITLE = new StringFlag("farewell-subtitle", "");

    // --- environment / world control (batch B3) ---
    public static final Flag<Boolean> LIGHTNING = state("lightning");
    public static final Flag<Boolean> LAVA_FIRE = state("lava-fire");
    public static final Flag<Boolean> WATER_FLOW = state("water-flow");
    public static final Flag<Boolean> LAVA_FLOW = state("lava-flow");
    public static final Flag<Boolean> FIRE_BURN = state("fire-burn");
    public static final Flag<Boolean> BLOCK_SPREAD = state("block-spread");
    public static final Flag<Boolean> SNOW_FALL = state("snow-fall");
    public static final Flag<Boolean> SNOW_MELT = state("snow-melt");
    public static final Flag<Boolean> ICE_FORM = state("ice-form");
    public static final Flag<Boolean> ICE_MELT = state("ice-melt");
    public static final Flag<Boolean> FROSTED_ICE_FORM = state("frosted-ice-form");
    public static final Flag<Boolean> FROSTED_ICE_MELT = state("frosted-ice-melt");
    public static final Flag<Boolean> SOIL_DRY = state("soil-dry");
    public static final Flag<Boolean> CORAL_FADE = state("coral-fade");
    public static final Flag<Boolean> SNOWMAN_TRAILS = state("snowman-trails");

    // --- growth (batch B4) ---
    public static final Flag<Boolean> CROP_GROWTH = state("crop-growth");
    public static final Flag<Boolean> TREE_GROWTH = state("tree-growth");
    public static final Flag<Boolean> MUSHROOM_GROWTH = state("mushroom-growth");
    public static final Flag<Boolean> VINE_GROWTH = state("vine-growth");
    public static final Flag<Boolean> GRASS_SPREAD = state("grass-spread");
    public static final Flag<Boolean> MYCELIUM_SPREAD = state("mycelium-spread");
    public static final Flag<Boolean> SCULK_GROWTH = state("sculk-growth");
    public static final Flag<Boolean> BONE_MEAL = state("bone-meal");
    public static final Flag<Boolean> ENTITY_TRANSFORM = state("entity-transform");

    // --- fine spawns (batch B5) ---
    public static final Flag<Boolean> ANIMAL_SPAWNING = state("animal-spawning");
    public static final Flag<Boolean> MONSTER_SPAWNING = state("monster-spawning");
    public static final Flag<Boolean> SPAWNER_SPAWNING = state("spawner-spawning");
    public static final Flag<Boolean> PHANTOM_SPAWNING = state("phantom-spawning");
    public static final Flag<Boolean> SLIME_SPAWNING = state("slime-spawning");
    public static final Flag<Boolean> NATURAL_SPAWNING = state("natural-spawning");
    public static final Flag<Boolean> EGG_SPAWNING = state("egg-spawning");
    public static final Flag<Boolean> COMMAND_SPAWNING = state("command-spawning");
    public static final Flag<Boolean> RAID_SPAWNING = state("raid-spawning");
    public static final Flag<Boolean> PATROL_SPAWNING = state("patrol-spawning");
    public static final Flag<Boolean> PORTAL_SPAWNING = state("portal-spawning");
    public static final Flag<List<String>> DENY_SPAWN = new StringListFlag("deny-spawn", List.of());

    // --- fine damage & explosions (batch B6) ---
    public static final Flag<Boolean> VILLAGER_DAMAGE = state("villager-damage");
    public static final Flag<Boolean> MONSTER_DAMAGE = state("monster-damage");
    public static final Flag<Boolean> PET_DAMAGE = state("pet-damage");
    public static final Flag<Boolean> FIREWORK_DAMAGE = state("firework-damage");
    public static final Flag<Boolean> ENTITY_EXPLOSION_DAMAGE = state("entity-explosion-damage");
    public static final Flag<Boolean> CREEPER_EXPLOSION = state("creeper-explosion");
    public static final Flag<Boolean> TNT = state("tnt");
    public static final Flag<Boolean> GHAST_FIREBALL = state("ghast-fireball");
    public static final Flag<Boolean> WITHER_DAMAGE = state("wither-damage");
    public static final Flag<Boolean> ENDERDRAGON_BLOCK_DAMAGE = state("enderdragon-block-damage");
    public static final Flag<Boolean> POTION_SPLASH = state("potion-splash");
    public static final Flag<Boolean> MELEE_PVP = state("melee-pvp");
    public static final Flag<Boolean> PROJECTILE_PVP = state("projectile-pvp");

    // --- zone ---
    public static final Flag<Boolean> ENTRY = state("entry");
    public static final Flag<Boolean> EXIT = state("exit");
    public static final Flag<String> GREETING = new StringFlag("greeting", "");
    public static final Flag<String> FAREWELL = new StringFlag("farewell", "");
    public static final Flag<String> TITLE = new StringFlag("title", "");
    public static final Flag<String> SUBTITLE = new StringFlag("subtitle", "");
    public static final Flag<String> ACTION_BAR = new StringFlag("action-bar", "");

    private static final List<Flag<?>> ALL = List.of(
            BLOCK_BREAK, BLOCK_PLACE, INTERACT, CONTAINER_ACCESS,
            BUCKET_FILL, BUCKET_EMPTY, ARMOR_STAND, HANGING_BREAK, HANGING_PLACE,
            VEHICLE_PLACE, VEHICLE_DESTROY, CROP_TRAMPLE,
            REDSTONE, PISTON, FIRE_IGNITE, FIRE_SPREAD, FLUID_FLOW, LEAF_DECAY,
            BLOCK_EXPLOSION, ENTITY_EXPLOSION,
            MOB_SPAWNING, MOB_GRIEFING, DAMAGE_ANIMALS, MOB_DAMAGE,
            PVP, INVINCIBLE, FALL_DAMAGE, HUNGER, ITEM_DROP, ITEM_PICKUP,
            ENDERPEARL, CHORUS_FRUIT, KEEP_INVENTORY, EXP_DROP, CHAT,
            ELYTRA, FLY, TOTEM, COMMAND_BLACKLIST,
            RIDE, SLEEP, RESPAWN_ANCHOR, ITEM_FRAME_ROTATION, USE_ANVIL, BEACON,
            VILLAGER_TRADE, SHEAR, LEASH, ANIMAL_BREEDING, SIGN_EDIT, FISHING_HOOK,
            PROJECTILE_LAUNCH, RECEIVE_CHAT, COMMAND_WHITELIST,
            DENY_MESSAGE, ENTRY_DENY_MESSAGE, EXIT_DENY_MESSAGE, FAREWELL_TITLE, FAREWELL_SUBTITLE,
            LIGHTNING, LAVA_FIRE, WATER_FLOW, LAVA_FLOW, FIRE_BURN, BLOCK_SPREAD,
            SNOW_FALL, SNOW_MELT, ICE_FORM, ICE_MELT, FROSTED_ICE_FORM, FROSTED_ICE_MELT,
            SOIL_DRY, CORAL_FADE, SNOWMAN_TRAILS,
            CROP_GROWTH, TREE_GROWTH, MUSHROOM_GROWTH, VINE_GROWTH, GRASS_SPREAD,
            MYCELIUM_SPREAD, SCULK_GROWTH, BONE_MEAL, ENTITY_TRANSFORM,
            ANIMAL_SPAWNING, MONSTER_SPAWNING, SPAWNER_SPAWNING, PHANTOM_SPAWNING, SLIME_SPAWNING,
            NATURAL_SPAWNING, EGG_SPAWNING, COMMAND_SPAWNING, RAID_SPAWNING, PATROL_SPAWNING,
            PORTAL_SPAWNING, DENY_SPAWN,
            VILLAGER_DAMAGE, MONSTER_DAMAGE, PET_DAMAGE, FIREWORK_DAMAGE, ENTITY_EXPLOSION_DAMAGE,
            CREEPER_EXPLOSION, TNT, GHAST_FIREBALL, WITHER_DAMAGE, ENDERDRAGON_BLOCK_DAMAGE,
            POTION_SPLASH, MELEE_PVP, PROJECTILE_PVP,
            ENTRY, EXIT, GREETING, FAREWELL, TITLE, SUBTITLE, ACTION_BAR);

    private Flags() {
    }

    /** All built-in flags, in registration/display order. */
    public static List<Flag<?>> all() {
        return ALL;
    }

    public static void registerAll(FlagRegistry registry) {
        ALL.forEach(registry::register);
    }

    private static Flag<Boolean> state(String key) {
        return new StateFlag(key, true);
    }
}
