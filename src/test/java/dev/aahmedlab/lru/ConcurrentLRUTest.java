package dev.aahmedlab.lru;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ConcurrentLRUTest {

  @Test
  void testBasicGetPut() {
    ConcurrentLRU lru = new ConcurrentLRU(2);
    lru.put(1, 10);
    lru.put(2, 20);

    assertEquals(10, lru.get(1));
    assertEquals(20, lru.get(2));
    assertEquals(-1, lru.get(3), "Missing key returns the -1 sentinel");
  }

  @Test
  void testEviction() {
    ConcurrentLRU lru = new ConcurrentLRU(2);
    lru.put(1, 10);
    lru.put(2, 20);
    lru.put(3, 30); // evicts least-recently-used key 1

    assertEquals(-1, lru.get(1));
    assertEquals(20, lru.get(2));
    assertEquals(30, lru.get(3));
    assertEquals(2, lru.size());
  }

  @Test
  void testAccessOrderProtectsRecentlyUsed() {
    ConcurrentLRU lru = new ConcurrentLRU(2);
    lru.put(1, 10);
    lru.put(2, 20);
    lru.get(1); // touch key 1 so key 2 becomes LRU
    lru.put(3, 30); // evicts key 2

    assertEquals(10, lru.get(1));
    assertEquals(-1, lru.get(2));
    assertEquals(30, lru.get(3));
  }

  @Test
  void testUpdateExistingValue() {
    ConcurrentLRU lru = new ConcurrentLRU(2);
    lru.put(1, 10);
    lru.put(1, 100);

    assertEquals(100, lru.get(1));
    assertEquals(1, lru.size(), "Updating must not grow the cache");
  }

  @Test
  void testContainsKeyAndClear() {
    ConcurrentLRU lru = new ConcurrentLRU(2);
    lru.put(1, 10);
    assertTrue(lru.containsKey(1));
    assertFalse(lru.containsKey(2));

    lru.clear();
    assertEquals(0, lru.size());
    assertEquals(-1, lru.get(1));
    lru.put(5, 50); // still usable after clear
    assertEquals(50, lru.get(5));
  }

  @Test
  void testInvalidCapacity() {
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> new ConcurrentLRU(0));
    assertTrue(ex.getMessage().contains("Capacity must be > 0"));
  }

  @RepeatedTest(5)
  void testConcurrentPutsNeverExceedCapacity() throws InterruptedException {
    int capacity = 50;
    int numThreads = 16;
    int opsPerThread = 500;
    ConcurrentLRU lru = new ConcurrentLRU(capacity);

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(numThreads);

    for (int t = 0; t < numThreads; t++) {
      final int base = t * opsPerThread;
      executor.submit(
          () -> {
            try {
              start.await();
              for (int i = 0; i < opsPerThread; i++) {
                int key = base + i;
                lru.put(key, key);
                lru.get(key);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              done.countDown();
            }
          });
    }

    start.countDown();
    assertTrue(done.await(10, TimeUnit.SECONDS), "Workers should finish");
    executor.shutdown();

    assertEquals(capacity, lru.size(), "Size must never exceed capacity under concurrency");
  }
}
