package dev.aahmedlab.ratelimiter;

/**
 * A thread-safe sliding-window-counter rate limiter that approximates the number of requests in any
 * rolling window of {@code windowSizeMillis} milliseconds and allows up to {@code capacity} of
 * them.
 *
 * <p>This class maintains three coordinated pieces of state:
 *
 * <ul>
 *   <li>{@code currentCount} — requests admitted in the current fixed window
 *   <li>{@code previousCount} — requests admitted in the immediately preceding window
 *   <li>{@code currentWindowStart} — the start timestamp of the current fixed window
 * </ul>
 *
 * <p><b>Concurrency Model:</b> A single intrinsic lock guards every call to {@link
 * #allowRequest()}. Rolling the window forward, computing the weighted estimate, and incrementing
 * the counter form one invariant, so the whole operation is mutually exclusive.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Weighted estimate:</b> The previous window's count is scaled by the fraction of it still
 *       overlapping the rolling window ({@code previousCount * (1 - elapsed/window) +
 *       currentCount}), smoothing the abrupt boundary bursts of a fixed window.
 *   <li><b>O(1) memory:</b> Only two counters and a timestamp are kept, regardless of request rate,
 *       unlike the exact {@link SlidingWindowLog}.
 *   <li><b>Multi-window gap handling:</b> If more than one window has elapsed since the last
 *       request, both counters reset rather than carrying stale data forward.
 * </ul>
 */
public class SlidingWindowCounter {
    private final int capacity;
    private final long windowSizeMillis;
    private final Object lock = new Object();
    private long currentWindowStart;
    private long currentCount = 0;
    private long previousCount = 0;

    public SlidingWindowCounter(int capacity, long windowSizeMillis) {
        if (capacity <= 0 || windowSizeMillis <= 0)
            throw new IllegalArgumentException("capacity and windowSizeMillis cannot be <= 0");
        this.capacity = capacity;
        this.windowSizeMillis = windowSizeMillis;
        this.currentWindowStart = System.currentTimeMillis();
    }

    public boolean allowRequest() {
        synchronized (lock) {
            long now = System.currentTimeMillis();
            advanceWindow(now);

            double elapsedInCurrent = now - currentWindowStart;
            double previousWeight = (windowSizeMillis - elapsedInCurrent) / windowSizeMillis;
            double estimatedCount = previousCount * previousWeight + currentCount;

            if (estimatedCount < capacity) {
                currentCount++;
                return true;
            }
            return false;
        }
    }

    private void advanceWindow(long now) {
        long elapsedWindows = (now - currentWindowStart) / windowSizeMillis;
        if (elapsedWindows == 1) {
            previousCount = currentCount;
            currentCount = 0;
            currentWindowStart += windowSizeMillis;
        } else if (elapsedWindows > 1) {
            previousCount = 0;
            currentCount = 0;
            currentWindowStart += elapsedWindows * windowSizeMillis;
        }
    }
}
