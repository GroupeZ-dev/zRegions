package fr.maxlego08.zregions.common.flag;

import fr.maxlego08.zregions.api.flag.Flag;
import fr.maxlego08.zregions.api.flag.FlagRegistry;

/**
 * The built-in flags. All default to allow — a freshly created region changes
 * nothing until an owner denies something. Registered once at startup, before
 * regions are loaded (stored values need the registry to parse).
 */
public final class Flags {

    public static final Flag<Boolean> BLOCK_BREAK = new StateFlag("block-break", true);
    public static final Flag<Boolean> BLOCK_PLACE = new StateFlag("block-place", true);
    public static final Flag<Boolean> INTERACT = new StateFlag("interact", true);
    public static final Flag<Boolean> CONTAINER_ACCESS = new StateFlag("container-access", true);
    public static final Flag<Boolean> PVP = new StateFlag("pvp", true);
    public static final Flag<Boolean> ENTRY = new StateFlag("entry", true);

    private Flags() {
    }

    public static void registerAll(FlagRegistry registry) {
        registry.register(BLOCK_BREAK);
        registry.register(BLOCK_PLACE);
        registry.register(INTERACT);
        registry.register(CONTAINER_ACCESS);
        registry.register(PVP);
        registry.register(ENTRY);
    }
}
