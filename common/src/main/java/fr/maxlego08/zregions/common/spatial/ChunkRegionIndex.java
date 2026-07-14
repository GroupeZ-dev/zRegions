package fr.maxlego08.zregions.common.spatial;

import fr.maxlego08.zregions.api.region.Region;
import fr.maxlego08.zregions.api.shape.BoundingBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-world spatial index keyed by chunk. THE performance piece (the anti-UltraRegions):
 * a lookup only ever tests the few regions overlapping one chunk, never the whole set.
 *
 * <p>Huge regions (over {@link #LARGE_THRESHOLD_CHUNKS} chunks) are kept out of the
 * buckets and tested linearly — there are few of them by nature (world-wide zones).</p>
 *
 * <p>Thread-safety: mutations only happen on cache reloads (rare, admin-driven) and
 * swap immutable bucket lists; queries are lock-free reads. Global (world-wide)
 * regions are not indexed here — the manager applies them as fallback.</p>
 */
public final class ChunkRegionIndex {

    public static final int LARGE_THRESHOLD_CHUNKS = 1024;

    private final Map<Long, List<Region>> byChunk = new HashMap<>();
    private final List<Region> largeRegions = new CopyOnWriteArrayList<>();
    private volatile Map<Long, List<Region>> snapshot = Map.of();

    /** Adds a region to the index. Must be called under the manager's write lock. */
    public void add(Region region) {
        BoundingBox box = region.getShape().getBoundingBox();
        if (box.chunkCount() > LARGE_THRESHOLD_CHUNKS) {
            this.largeRegions.add(region);
            return;
        }
        for (int chunkX = box.minChunkX(); chunkX <= box.maxChunkX(); chunkX++) {
            for (int chunkZ = box.minChunkZ(); chunkZ <= box.maxChunkZ(); chunkZ++) {
                this.byChunk.computeIfAbsent(BoundingBox.chunkKey(chunkX, chunkZ), key -> new ArrayList<>(2)).add(region);
            }
        }
        publish();
    }

    /** Removes a region from the index. Must be called under the manager's write lock. */
    public void remove(Region region) {
        if (this.largeRegions.remove(region)) {
            return;
        }
        BoundingBox box = region.getShape().getBoundingBox();
        for (int chunkX = box.minChunkX(); chunkX <= box.maxChunkX(); chunkX++) {
            for (int chunkZ = box.minChunkZ(); chunkZ <= box.maxChunkZ(); chunkZ++) {
                long key = BoundingBox.chunkKey(chunkX, chunkZ);
                List<Region> bucket = this.byChunk.get(key);
                if (bucket != null) {
                    bucket.remove(region);
                    if (bucket.isEmpty()) {
                        this.byChunk.remove(key);
                    }
                }
            }
        }
        publish();
    }

    public void clear() {
        this.byChunk.clear();
        this.largeRegions.clear();
        publish();
    }

    private void publish() {
        Map<Long, List<Region>> copy = new HashMap<>((int) (this.byChunk.size() / 0.75f) + 1);
        for (Map.Entry<Long, List<Region>> entry : this.byChunk.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        this.snapshot = copy;
    }

    /**
     * All regions whose precise shape contains the point, unsorted. HOT PATH:
     * lock-free, zero allocation when the chunk is empty and there are no large regions.
     */
    public List<Region> query(double x, double y, double z) {
        List<Region> bucket = this.snapshot.get(BoundingBox.chunkKey(((int) Math.floor(x)) >> 4, ((int) Math.floor(z)) >> 4));
        if ((bucket == null || bucket.isEmpty()) && this.largeRegions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Region> result = null;
        if (bucket != null) {
            for (Region region : bucket) {
                if (region.getShape().contains(x, y, z)) {
                    if (result == null) result = new ArrayList<>(4);
                    result.add(region);
                }
            }
        }
        for (Region region : this.largeRegions) {
            if (region.getShape().contains(x, y, z)) {
                if (result == null) result = new ArrayList<>(4);
                result.add(region);
            }
        }
        return result == null ? Collections.emptyList() : result;
    }
}
