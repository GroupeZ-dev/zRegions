package fr.maxlego08.zregions.common.selection;

import fr.maxlego08.zregions.common.platform.RegionLocation;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks in-progress region selections per player. Selections are immutable
 * snapshots: every position update swaps in a fresh {@link Selection}, so readers
 * never observe a half-updated pair.
 */
public final class SelectionManager {

    private final Map<UUID, Selection> selections = new ConcurrentHashMap<>();

    public void setPos1(UUID playerId, RegionLocation location) {
        this.selections.compute(playerId, (id, current) ->
                new Selection(location, current == null ? null : current.getPos2()));
    }

    public void setPos2(UUID playerId, RegionLocation location) {
        this.selections.compute(playerId, (id, current) ->
                new Selection(current == null ? null : current.getPos1(), location));
    }

    public Optional<Selection> getSelection(UUID playerId) {
        return Optional.ofNullable(this.selections.get(playerId));
    }

    public void clear(UUID playerId) {
        this.selections.remove(playerId);
    }
}
