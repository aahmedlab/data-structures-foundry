package dev.aahmedlab.ratelimiter;

public class TokenBucket {
  private final int capacity;
  private final int refillRate; // Per second
  private final long millisPerToken; // 0 means no refill
  private long bucket;
  private long lastRefillTime;

  public TokenBucket(int capacity, int refillRate) {
    if (capacity <= 0 || refillRate < 0)
      throw new IllegalArgumentException("capacity must be > 0 and refillRate must be >= 0");
    this.capacity = capacity;
    this.refillRate = refillRate;
    this.bucket = capacity;
    this.millisPerToken = refillRate == 0 ? 0 : 1000L / refillRate;
    this.lastRefillTime = System.currentTimeMillis();
  }

  public boolean allowRequest() {
    long now = System.currentTimeMillis();
    if (refillRate > 0) {
      long elapsed = now - lastRefillTime;
      long tokensToAdd = elapsed / millisPerToken;
      if (tokensToAdd > 0) {
        bucket = Math.min(capacity, bucket + tokensToAdd);
        // Only advance by the time actually consumed, preserving the sub-token remainder.
        lastRefillTime += tokensToAdd * millisPerToken;
      }
    }
    if (bucket >= 1) {
      bucket -= 1;
      return true;
    }

    return false;
  }
}
