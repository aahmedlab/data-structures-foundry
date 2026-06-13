package dev.aahmedlab.ratelimiter;

public class ConcurrentTokenBucket {
  private final int capacity;
  private long bucket;
  private final long millisPerToken;
  private long lastRefillTime;
  private final Object lock = new Object();

  public ConcurrentTokenBucket(int capacity, int refillRate) {
    if (capacity <= 0 || refillRate < 0)
      throw new IllegalArgumentException("capacity must be > 0 and refillRate must be >= 0");
    this.capacity = capacity;
    this.bucket = capacity;
    this.lastRefillTime = System.currentTimeMillis();
    this.millisPerToken = refillRate == 0 ? 0 : 1000 / refillRate; // 0 means no refill
  }

  public boolean allowRequest() {
    synchronized (lock) {
      long now = System.currentTimeMillis();
      if (millisPerToken > 0) {
        long elapsed = (now - lastRefillTime);
        long tokensToAdd = elapsed / millisPerToken;
        if (tokensToAdd > 0) {
          bucket = Math.min(capacity, bucket + tokensToAdd);
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
}
