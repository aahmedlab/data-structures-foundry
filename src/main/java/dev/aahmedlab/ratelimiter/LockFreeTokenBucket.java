package dev.aahmedlab.ratelimiter;

import java.util.concurrent.atomic.AtomicReference;

public class LockFreeTokenBucket {
    private final long capacity;
    private final long millisPerToken;
    private final AtomicReference<BucketState> state;

    public LockFreeTokenBucket(int capacity, int refillRate) {
        if (capacity <= 0 || refillRate <= 0)
            throw new IllegalArgumentException("capacity and refillRate cannot be <= 0");
        this.capacity = capacity;
        BucketState initState = new BucketState(capacity, System.currentTimeMillis());
        state = new AtomicReference<>(initState);
        this.millisPerToken = 1000 / refillRate;
    }

    public boolean allowRequest() {
        while (true) {
            BucketState current = state.get();
            long now = System.currentTimeMillis();
            long elapsed = (now - current.lastRefill);
            long tokensToAdd = elapsed / millisPerToken;
            long newTokens = Math.min(capacity, current.tokens + tokensToAdd);
            long lastRefillTime = current.lastRefill + tokensToAdd * millisPerToken;
            if (newTokens < 1) {
                return false;
            }
            BucketState nextState = new BucketState(newTokens - 1, lastRefillTime);
            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }

    private record BucketState(long tokens, long lastRefill) {
    }
}
