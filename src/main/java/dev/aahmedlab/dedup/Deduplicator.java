package dev.aahmedlab.dedup;

import java.time.Clock;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A single-threaded TTL-based deduplicator that reports whether a key is being seen for the first
 * time within a configurable time-to-live window.
 *
 * <p>This class maintains two coordinated data structures:
 *
 * <ul>
 *   <li>A {@link HashMap} mapping each key to the epoch second at which it was last first-seen
 *   <li>A ring of {@link HashSet} buckets (one per second of TTL) used by {@link #sweeper()} to
 *       expire keys incrementally without scanning the whole cache
 * </ul>
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Bucketed expiry:</b> Keys are placed in the bucket {@code timestamp % ttl}, so the
 *       sweeper only needs to clear the buckets that have elapsed since the last sweep.
 *   <li><b>Lazy expiry in {@link #isFirstSeen(Object)}:</b> A key older than the TTL is treated as
 *       first-seen again even if the sweeper has not yet run.
 *   <li><b>Injectable {@link Clock}:</b> Enables deterministic testing of time-dependent behavior.
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is <em>not</em> thread-safe. See {@link
 * ConcurrentDeduplicator} for a concurrent variant.
 *
 * @param <K> the type of keys being deduplicated
 */
@SuppressWarnings("unchecked")
public class Deduplicator<K> {
    private final Map<K, Long> cache = new HashMap<>();
    private final Set<K>[] hits;
    private final Clock clock;
    private final int ttl;
    private long lastSweptSecond;

    public Deduplicator(int ttl) {
        this(ttl, Clock.systemUTC());
    }

    public Deduplicator(int ttl, Clock clock) {
        this.clock = clock;
        hits = new HashSet[ttl];
        this.ttl = ttl;
        lastSweptSecond = clock.instant().getEpochSecond();
        for (int i = 0; i < ttl; i++) {
            hits[i] = new HashSet<>();
        }
    }

    public boolean isFirstSeen(K key) {
        long currentTimeSeconds = clock.instant().getEpochSecond();
        int index = (int) (currentTimeSeconds % hits.length);
        if (!cache.containsKey(key)) {
            cache.put(key, currentTimeSeconds);
            hits[index].add(key);
            return true;
        } else {
            long createdTimeSeconds = cache.get(key);
            int oldIndex = (int) (createdTimeSeconds % hits.length);
            if (currentTimeSeconds - createdTimeSeconds > this.ttl) {
                cache.replace(key, createdTimeSeconds, currentTimeSeconds);
                hits[oldIndex].remove(key);
                hits[index].add(key);
                return true;
            }
            return false;
        }
    }

    public void sweeper() {
        long currentSecond = clock.instant().getEpochSecond();
        for (long t = lastSweptSecond + 1; t <= currentSecond; t++) {
            int bucketIndex = (int) (t % hits.length);
            for (K key : hits[bucketIndex]) {
                cache.remove(key);
            }
            hits[bucketIndex].clear();
        }
        lastSweptSecond = currentSecond;
    }
}
