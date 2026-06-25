package dev.aahmedlab.ratelimiter;

/**
 * A single-threaded fixed-window rate limiter that allows up to {@code capacity} requests within
 * each non-overlapping window of {@code windowSizeMillis} milliseconds.
 *
 * <p>This class maintains two pieces of state:
 *
 * <ul>
 *   <li>{@code tokens} — the number of requests admitted in the current window
 *   <li>{@code windowStartTime} — the start timestamp of the current window
 * </ul>
 *
 * <p><b>Concurrency Model:</b> None. This implementation is <b>not thread-safe</b>: the window
 * reset and the check-then-increment of {@code tokens} are unsynchronized, so concurrent callers
 * can exceed the limit or observe stale state. Use {@link ConcurrentFixedWindow} when a shared
 * limiter is accessed from multiple threads.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Hard window reset:</b> Counting restarts at each boundary, which is simple and O(1) but
 *       permits up to {@code 2 * capacity} requests across a boundary (the fixed-window burst that
 *       {@link SlidingWindowCounter} smooths).
 *   <li><b>No locking:</b> Kept lock-free for clarity as the baseline single-threaded variant; see
 *       {@link ConcurrentFixedWindow} for the synchronized counterpart.
 * </ul>
 */
public class FixedWindow {
  private final int capacity;
  private final long windowSizeMillis;
  private long tokens = 0;
  private long windowStartTime;

  public FixedWindow(int capacity, long windowSizeMillis) {
    if (capacity <= 0 || windowSizeMillis <= 0)
      throw new IllegalArgumentException("capacity and windowSizeMillis cannot be <= 0");
    this.capacity = capacity;
    this.windowSizeMillis = windowSizeMillis;
    this.windowStartTime = System.currentTimeMillis();
  }

  public boolean allowRequest() {
    long now = System.currentTimeMillis();
    long elapsed = (now - windowStartTime);
    if (elapsed >= windowSizeMillis) {
      windowStartTime = now;
      tokens = 0;
    }
    if (tokens < capacity) {
      tokens++;
      return true;
    }
    return false;
  }
}
