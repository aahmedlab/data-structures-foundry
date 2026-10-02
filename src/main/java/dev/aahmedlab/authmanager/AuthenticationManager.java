package dev.aahmedlab.authmanager;

import java.util.*;

class AuthenticationManager {

  private static class Entry {
    String tokenId;
    int expiry;

    Entry(String tokenId, int expiry) {
      this.tokenId = tokenId;
      this.expiry = expiry;
    }
  }

  private final int ttl;

  // Authoritative current expiration for each active token
  private final Map<String, Integer> expiryByToken = new HashMap<>();

  // May contain stale entries after renewals
  private final PriorityQueue<Entry> expiryHeap =
      new PriorityQueue<>(Comparator.comparingInt(e -> e.expiry));

  private int activeCount = 0;

  public AuthenticationManager(int timeToLive) {
    this.ttl = timeToLive;
  }

  public void generate(String tokenId, int currentTime) {
    int expiry = currentTime + ttl;

    // Any token still in the map is already counted, even if
    // it has expired but not been cleaned up yet.
    if (expiryByToken.put(tokenId, expiry) == null) {
      activeCount++;
    }
    expiryHeap.offer(new Entry(tokenId, expiry));
  }

  public void renew(String tokenId, int currentTime) {
    Integer currentExpiry = expiryByToken.get(tokenId);

    // Doesn't exist
    if (currentExpiry == null) {
      return;
    }

    // Exists physically, but has already expired
    if (currentExpiry <= currentTime) {
      expiryByToken.remove(tokenId);
      activeCount--;
      return;
    }

    // Still active: append a new expiration record.
    // Do NOT remove the old heap entry.
    int newExpiry = currentTime + ttl;

    expiryByToken.put(tokenId, newExpiry);
    expiryHeap.offer(new Entry(tokenId, newExpiry));
  }

  public int countUnexpiredTokens(int currentTime) {
    cleanup(currentTime);
    return activeCount;
  }

  private void cleanup(int currentTime) {

    while (!expiryHeap.isEmpty() && expiryHeap.peek().expiry <= currentTime) {

      Entry entry = expiryHeap.poll();

      Integer currentExpiry = expiryByToken.get(entry.tokenId);

      // Token was already removed.
      if (currentExpiry == null) {
        continue;
      }

      // Current authoritative token is also expired.
      if (currentExpiry <= currentTime) {
        expiryByToken.remove(entry.tokenId);
        activeCount--;
      }

      // Otherwise:
      // currentExpiry > currentTime
      // Therefore this heap entry was stale from
      // an earlier renewal. Just discard it.
    }
  }
}
