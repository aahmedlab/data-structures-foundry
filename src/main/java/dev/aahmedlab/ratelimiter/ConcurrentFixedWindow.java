package dev.aahmedlab.ratelimiter;

/**
 * A thread-safe fixed-window rate limiter that allows up to {@code capacity} requests within each
 * non-overlapping window of {@code windowSizeMillis} milliseconds.
 *
 * <p>This class maintains two coordinated pieces of state:
 *
 * <ul>
 *   <li>{@code tokens} — the number of requests admitted in the current window
 *   <li>{@code windowStartTime} — the start timestamp of the current window
 * </ul>
 *
 * <p><b>Concurrency Model:</b> A single intrinsic lock guards every call to {@link
 * #allowRequest()}. The window reset and the check-then-increment of {@code tokens} form one
 * invariant, so the whole operation is mutually exclusive.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Single lock:</b> The reset and the increment must be atomic; without it concurrent
 *       threads could both pass the {@code tokens < capacity} check and exceed the limit.
 *   <li><b>Hard window reset:</b> Counting restarts at each boundary, which is simple and O(1) but
 *       permits up to {@code 2 * capacity} requests across a boundary (the fixed-window burst that
 *       {@link SlidingWindowCounter} smooths).
 * </ul>
 */
public class ConcurrentFixedWindow {
  private final int capacity;
  private final long windowSizeMillis;
  private final Object lock = new Object();
  private long tokens = 0;
  private long windowStartTime;

  public ConcurrentFixedWindow(int capacity, long windowSizeMillis) {
    if (capacity <= 0 || windowSizeMillis <= 0)
      throw new IllegalArgumentException("capacity and windowSizeMillis cannot be <= 0");
    this.capacity = capacity;
    this.windowSizeMillis = windowSizeMillis;
    this.windowStartTime = System.currentTimeMillis();
  }

  public boolean allowRequest() {
    synchronized (lock) {
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
}
