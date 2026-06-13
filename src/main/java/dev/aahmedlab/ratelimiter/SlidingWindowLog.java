package dev.aahmedlab.ratelimiter;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A thread-safe sliding-window-log rate limiter that allows up to {@code capacity} requests within
 * any rolling window of {@code windowSizeMillis} milliseconds.
 *
 * <p>This class maintains a single piece of state:
 *
 * <ul>
 *   <li>A {@link Deque} of request timestamps in ascending order, holding only the timestamps that
 *       still fall inside the current rolling window
 * </ul>
 *
 * <p><b>Concurrency Model:</b> A single intrinsic lock guards every call to {@link
 * #allowRequest()}. Eviction of expired timestamps and the size check that admits or rejects a
 * request form one invariant, so the whole operation is mutually exclusive.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Exact accounting:</b> Storing every in-window timestamp gives precise limiting with no
 *       boundary bursts, at the cost of memory proportional to the request rate.
 *   <li><b>{@link ArrayDeque} backbone:</b> O(1) append at the tail and eviction from the head keep
 *       {@link #allowRequest()} amortized O(1) per call.
 *   <li><b>Single lock:</b> The evict-then-check sequence must be atomic; splitting it would allow
 *       the limit to be exceeded under contention.
 * </ul>
 */
public class SlidingWindowLog {
  private final int capacity;
  private final long windowSizeMillis;
  private final Deque<Long> timestamps = new ArrayDeque<>();
  private final Object lock = new Object();

  public SlidingWindowLog(int capacity, long windowSizeMillis) {
    if (capacity <= 0 || windowSizeMillis <= 0)
      throw new IllegalArgumentException("capacity and windowSizeMillis cannot be <= 0");
    this.capacity = capacity;
    this.windowSizeMillis = windowSizeMillis;
  }

  public boolean allowRequest() {
    synchronized (lock) {
      long now = System.currentTimeMillis();
      long windowStart = now - windowSizeMillis;
      while (!timestamps.isEmpty() && timestamps.peekFirst() <= windowStart) {
        timestamps.pollFirst();
      }
      if (timestamps.size() < capacity) {
        timestamps.addLast(now);
        return true;
      }
      return false;
    }
  }
}
