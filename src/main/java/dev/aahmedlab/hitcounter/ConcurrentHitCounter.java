package dev.aahmedlab.hitcounter;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A thread-safe sliding-window hit counter that records hits per timestamp and returns the total
 * number of hits within the last {@code capacity} seconds.
 *
 * <p>This class maintains two coordinated pieces of state:
 *
 * <ul>
 *   <li>A circular array of {@link AtomicReference}-wrapped immutable {@link Hit} records, indexed
 *       by {@code timestamp % capacity}
 *   <li>An {@link AtomicInteger} running total, adjusted on every slot overwrite or expiry so
 *       queries do not require summing the array
 * </ul>
 *
 * <p><b>Concurrency Model:</b> Lock-free. Each slot is updated via a compare-and-set (CAS) retry
 * loop: a thread reads the current {@link Hit}, builds the replacement, and retries until the CAS
 * succeeds. Because {@link Hit} is an immutable record, a successful CAS publishes a fully
 * consistent snapshot.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>CAS retry loops:</b> Avoid blocking entirely; contention on a slot only costs a re-read
 *       and retry rather than a context switch.
 *   <li><b>Immutable {@link Hit} records:</b> Replacing the whole record atomically prevents torn
 *       reads of the (timestamp, count) pair.
 *   <li><b>Weakly consistent total:</b> {@code totalHits} is adjusted after the slot CAS, so {@link
 *       #getHit(int)} may briefly observe a total that lags concurrent updates; it converges once
 *       in-flight operations complete.
 *   <li><b>Lazy expiry in {@link #getHit(int)}:</b> Expired slots are CAS-nulled at query time and
 *       their counts subtracted, keeping {@link #hit(int)} O(1).
 * </ul>
 */
public class ConcurrentHitCounter {
  private AtomicReference<Hit>[] hits;
  private AtomicInteger totalHits;
  private final int capacity;

  public ConcurrentHitCounter(int capacity) {
    this.capacity = capacity;
    this.hits = new AtomicReference[capacity];
    for (int i = 0; i < capacity; i++) {
      this.hits[i] = new AtomicReference<>(null);
    }
    this.totalHits = new AtomicInteger(0);
  }

  public void hit(int timestamp) {
    int index = timestamp % capacity;

    Hit currentHit;
    Hit next;
    do {
      currentHit = hits[index].get();
      next = buildNext(currentHit, timestamp);
    } while (!hits[index].compareAndSet(currentHit, next));

    if (currentHit == null || currentHit.timestamp() == timestamp) {
      totalHits.incrementAndGet();
    } else {
      totalHits.addAndGet(1 - currentHit.count());
    }
  }

  private Hit buildNext(Hit currentHit, int timestamp) {
    if (currentHit != null && currentHit.timestamp() == timestamp) {
      return new Hit(timestamp, currentHit.count() + 1);
    } else {
      return new Hit(timestamp, 1);
    }
  }

  public int getHit(int timestamp) {
    for (int i = 0; i < capacity; i++) {
      while (true) {
        Hit currentHit = hits[i].get();
        if (currentHit == null) break;
        int diff = timestamp - currentHit.timestamp();
        if (diff < capacity) break;
        if (hits[i].compareAndSet(currentHit, null)) {
          totalHits.addAndGet(-currentHit.count());
          break;
        }
      }
    }
    return totalHits.get();
  }
}
