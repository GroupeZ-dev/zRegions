package fr.maxlego08.zregions.api.region;

/**
 * How a player reached a position, relative to a region. The movement engine uses
 * it to distinguish leaving on foot from leaving by teleport — so a region may
 * forbid walking out yet still let a teleport escape, never trapping a player.
 *
 * <p>Public contract: region enter/exit events carry it so addons can react to
 * <em>how</em> a player crossed a border.</p>
 */
public enum MovementCause {

    /** Ordinary movement — walking, running, jumping, swimming. */
    WALK,

    /** A teleport: commands, plugins, ender pearls, chorus fruit, and the like. */
    TELEPORT,

    /** A nether or end portal transition. */
    PORTAL
}
