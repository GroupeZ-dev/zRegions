package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks in-progress region selections per player. Selections are immutable
 * snapshots: every update swaps in a fresh {@link Selection}, so readers never
 * observe a half-updated state.
 */
public final class SelectionManager {

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public void setPos1(UUID playerId, RegionLocation location) {
        this.selections.compute(playerId, (id, current) -> new Selection(location,
                current == null ? null : current.getPos2(),
                current == null ? List.of() : current.getPoints()));
    }

    public void setPos2(UUID playerId, RegionLocation location) {
        this.selections.compute(playerId, (id, current) -> new Selection(
                current == null ? null : current.getPos1(), location,
                current == null ? List.of() : current.getPoints()));
    }

    /** Appends a polygon vertex; returns the new vertex count. */
    public int addPoint(UUID playerId, RegionLocation location) {
        Selection updated = this.selections.compute(playerId, (id, current) -> {
            List<RegionLocation> points = current == null
                    ? new ArrayList<>() : new ArrayList<>(current.getPoints());
            points.add(location);
            return new Selection(current == null ? null : current.getPos1(),
                    current == null ? null : current.getPos2(), points);
        });
        return updated.getPoints().size();
    }

    /** Replaces the whole vertex list (star generator). */
    public void setPoints(UUID playerId, List<RegionLocation> points) {
        this.selections.compute(playerId, (id, current) -> new Selection(
                current == null ? null : current.getPos1(),
                current == null ? null : current.getPos2(), points));
    }

    /** Empties the vertex list, keeping both positions. */
    public void clearPoints(UUID playerId) {
        this.selections.computeIfPresent(playerId, (id, current) ->
                new Selection(current.getPos1(), current.getPos2()));
    }

    public Optional<Selection> getSelection(UUID playerId) {
        return Optional.ofNullable(this.selections.get(playerId));
    }

    public void clear(UUID playerId) {
        this.selections.remove(playerId);
    }
}
