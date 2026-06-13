package dev.aahmedlab.hitcounter;

/**
 * A single-threaded sliding-window hit counter that records hits per timestamp and returns the
 * total number of hits within the last {@code capacity} seconds.
 *
 * <p>This class maintains two coordinated pieces of state:
 *
 * <ul>
 *   <li>A circular array of {@link Hit} records indexed by {@code timestamp % capacity}, each
 *       holding a timestamp and the number of hits at that timestamp
 *   <li>A running {@code totalHits} counter, adjusted incrementally on every slot overwrite or
 *       expiry so queries do not require summing the array
 * </ul>
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Circular buffer:</b> Bounds memory to {@code capacity} slots regardless of how many hits
 *       are recorded; an incoming timestamp reuses (and evicts) the slot of the timestamp it
 *       collides with.
 *   <li><b>Lazy expiry in {@link #getHit(int)}:</b> Slots older than the window are nulled out and
 *       subtracted from the running total at query time, keeping {@link #hit(int)} O(1).
 *   <li><b>Monotonic timestamps assumed:</b> Callers are expected to pass non-decreasing
 *       timestamps; out-of-order hits may overwrite newer slots.
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is <em>not</em> thread-safe. See {@link ConcurrentHitCounter}
 * for a concurrent variant.
 */
public class HitCounter {
  private Hit[] hits;
  private int totalHits;
  private final int capacity;

  public HitCounter(int capacity) {
    this.capacity = capacity;
    this.hits = new Hit[capacity];
    this.totalHits = 0;
  }

  public void hit(int timestamp) {
    int index = timestamp % capacity;
    int counter = 1;
    if (hits[index] != null && hits[index].timestamp() == timestamp) {
      counter = hits[index].count() + 1;
    } else if (hits[index] != null && hits[index].timestamp() != timestamp) {
      totalHits -= hits[index].count();
    }
    hits[index] = new Hit(timestamp, counter);
    totalHits++;
  }

  public int getHit(int timestamp) {
    for (int i = 0; i < capacity; i++) {
      if (hits[i] != null) {
        int diff = timestamp - hits[i].timestamp();
        if (diff >= capacity) {
          totalHits -= hits[i].count();
          hits[i] = null;
        }
      }
    }
    return totalHits;
  }
}
