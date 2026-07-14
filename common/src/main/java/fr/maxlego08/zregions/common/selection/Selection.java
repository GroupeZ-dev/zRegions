package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;
import fr.maxlego08.zregions.common.shape.CuboidShape;

import java.util.Optional;

/**
 * An immutable snapshot of a player's two selection positions. Either position may
 * be {@code null} while the selection is in progress; {@link #toCuboid()} only
 * yields a shape once both positions are set in the same world.
 */
public final class Selection {

    private final RegionLocation pos1;
    private final RegionLocation pos2;

    public Selection(RegionLocation pos1, RegionLocation pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    public RegionLocation getPos1() {
        return this.pos1;
    }

    public RegionLocation getPos2() {
        return this.pos2;
    }

    public boolean isComplete() {
        return this.pos1 != null && this.pos2 != null;
    }

    public boolean isSameWorld() {
        return isComplete() && this.pos1.getWorldName().equals(this.pos2.getWorldName());
    }

    /** The world of the first defined position, or {@code null} for an empty selection. */
    public String getWorldName() {
        if (this.pos1 != null) {
            return this.pos1.getWorldName();
        }
        return this.pos2 != null ? this.pos2.getWorldName() : null;
    }

    /** The selected cuboid, present only when the selection is complete and in a single world. */
    public Optional<CuboidShape> toCuboid() {
        if (!isSameWorld()) {
            return Optional.empty();
        }
        return Optional.of(new CuboidShape(
                this.pos1.getBlockX(), this.pos1.getBlockY(), this.pos1.getBlockZ(),
                this.pos2.getBlockX(), this.pos2.getBlockY(), this.pos2.getBlockZ()));
    }
}
