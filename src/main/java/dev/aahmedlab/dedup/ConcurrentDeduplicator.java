package dev.aahmedlab.dedup;

import java.time.Clock;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe TTL-based deduplicator that reports whether a key is being seen for the first time
 * within a configurable time-to-live window.
 *
 * <p>This class maintains two coordinated data structures:
 *
 * <ul>
 *   <li>A {@link ConcurrentHashMap} mapping each key to the epoch second at which it was last
 *       first-seen
 *   <li>A ring of concurrent {@link Set} buckets (one per second of TTL) used by {@link #sweeper()}
 *       to expire keys incrementally without scanning the whole cache
 * </ul>
 *
 * <p><b>Concurrency Model:</b> Relies on {@link ConcurrentHashMap#compute(Object,
 * java.util.function.BiFunction)} to atomically check-and-update a key's last-seen timestamp,
 * guaranteeing exactly one caller observes {@code true} per key per TTL window. Bucket membership
 * is updated inside the compute block, which executes under the map's per-bin lock for that key.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Atomic compute:</b> Avoids check-then-act races without a global lock, allowing high
 *       concurrency across distinct keys.
 *   <li><b>Bucketed expiry:</b> Keys are placed in the bucket {@code timestamp % ttl}, so the
 *       sweeper only needs to clear the buckets that have elapsed since the last sweep.
 *   <li><b>Conditional remove in sweeper:</b> {@code cache.remove(key, insertedAt)} only removes
 *       entries whose timestamp matches the expiring bucket, so refreshed keys survive.
 *   <li><b>Single-threaded sweeper:</b> {@link #sweeper()} must only be called from one thread
 *       (e.g. a single-thread ScheduledExecutorService); {@code lastSweptSecond} is volatile for
 *       visibility, not mutual exclusion.
 *   <li><b>Injectable {@link Clock}:</b> Enables deterministic testing of time-dependent behavior.
 * </ul>
 *
 * @param <K> the type of keys being deduplicated
 */
@SuppressWarnings("unchecked")
public class ConcurrentDeduplicator<K> {
  private final Map<K, Long> cache = new ConcurrentHashMap<>();
  private final Set<K>[] hits;
  private final Clock clock;
  private final int ttl;
  private volatile long lastSweptSecond;

  public ConcurrentDeduplicator(int ttl) {
    this(ttl, Clock.systemUTC());
  }

  public ConcurrentDeduplicator(int ttl, Clock clock) {
    this.clock = clock;
    hits = new Set[ttl];
    this.ttl = ttl;
    lastSweptSecond = clock.instant().getEpochSecond();
    for (int i = 0; i < ttl; i++) {
      hits[i] = ConcurrentHashMap.newKeySet();
    }
  }

  public boolean isFirstSeen(K key) {
    long currentTimeSeconds = clock.instant().getEpochSecond();
    int index = (int) (currentTimeSeconds % hits.length);
    boolean[] firstSeen = {false};
    cache.compute(
        key,
        (k, oldValue) -> {
          if (oldValue == null) {
            hits[index].add(k);
            firstSeen[0] = true;
            return currentTimeSeconds;
          }
          if (currentTimeSeconds - oldValue > this.ttl) {
            int oldIndex = (int) (oldValue % hits.length);
            hits[oldIndex].remove(k);
            hits[index].add(k);
            firstSeen[0] = true;
            return currentTimeSeconds;
          }
          return oldValue;
        });

    return firstSeen[0];
  }

  /**
   * Must only be called from a single thread. Schedule via a single-thread
   * ScheduledExecutorService.
   */
  public void sweeper() {
    long currentSecond = clock.instant().getEpochSecond();
    for (long t = lastSweptSecond + 1; t <= currentSecond; t++) {
      int bucketIndex = (int) (t % hits.length);
      long insertedAt = t - ttl; // when keys in this bucket were inserted
      for (K key : hits[bucketIndex]) {
        cache.remove(key, insertedAt);
      }
      hits[bucketIndex].clear();
    }
    lastSweptSecond = currentSecond;
  }

  int cacheSize() {
    return cache.size();
  }
}
