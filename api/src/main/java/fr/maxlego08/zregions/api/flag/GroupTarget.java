package fr.maxlego08.zregions.api.flag;

/**
 * Who a flag value applies to within a region. A flag may hold a different value
 * per target; resolution picks the most specific target matching the player
 * ({@link #OWNER} > {@link #MEMBER} > {@link #VISITOR}), then falls back to {@link #ALL}.
 *
 * <p>Per-permission-group targets (LuckPerms) are planned as an extension and will
 * not change this contract.</p>
 */
public enum GroupTarget {

    OWNER,
    MEMBER,
    VISITOR,
    ALL;

    public static GroupTarget parse(String input, GroupTarget def) {
        if (input == null) return def;
        try {
            return valueOf(input.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return def;
        }
    }
}
