package fr.maxlego08.zregions.api.region;

/**
 * The role of a player inside a region.
 */
public enum MemberRole {

    OWNER,
    MEMBER;

    public static MemberRole parse(String input, MemberRole def) {
        if (input == null) return def;
        try {
            return valueOf(input.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return def;
        }
    }
}
