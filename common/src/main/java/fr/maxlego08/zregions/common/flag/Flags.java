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
