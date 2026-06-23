package dev.aahmedlab.hitcounter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConcurrentHitCounterTest {

  private static final int CAPACITY = 5;
  private ConcurrentHitCounter counter;

  @BeforeEach
  void setUp() {
    counter = new ConcurrentHitCounter(CAPACITY);
  }

  @Test
  void testInitialState() {
    assertEquals(0, counter.getHit(1));
  }

  @Test
  void testSingleHit() {
    counter.hit(1);
    assertEquals(1, counter.getHit(1));
  }

  @Test
  void testMultipleHitsSameTimestamp() {
    counter.hit(1);
    counter.hit(1);
    counter.hit(1);
    assertEquals(3, counter.getHit(1));
  }

  @Test
  void testMultipleHitsDifferentTimestamps() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    assertEquals(3, counter.getHit(3));
  }

  @Test
  void testHitsAtDifferentIndices() {
    counter.hit(0);
    counter.hit(1);
    counter.hit(5);
    assertEquals(2, counter.getHit(5));
  }

  @Test
  void testCollisionReplacesOldTimestamp() {
    counter.hit(1);
    counter.hit(6);
    assertEquals(1, counter.getHit(6));
  }

  @Test
  void testCollisionAdjustsTotalHits() {
    counter.hit(1);
    counter.hit(2);
    assertEquals(2, counter.getHit(2));

    counter.hit(6);
    assertEquals(2, counter.getHit(6));
  }

  @Test
  void testOldHitsAreExpired() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    assertEquals(3, counter.getHit(3));

    assertEquals(2, counter.getHit(6));
  }

  @Test
  void testGetHitCleansExpiredEntries() {
    counter.hit(1);
    counter.hit(2);
    assertEquals(2, counter.getHit(2));

    assertEquals(1, counter.getHit(6));
    assertEquals(0, counter.getHit(7));
  }

  @Test
  void testGetHitCleansExpiredEntriesAndCountsCorrectly() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    counter.hit(4);
    counter.hit(5);
    assertEquals(5, counter.getHit(5));

    assertEquals(4, counter.getHit(6));

    assertEquals(3, counter.getHit(7));
  }

  @Test
  void testHitThenGetHitMultipleTimes() {
    counter.hit(1);
    counter.hit(2);
    assertEquals(2, counter.getHit(3));
    assertEquals(2, counter.getHit(4));
    assertEquals(2, counter.getHit(5));
  }

  @Test
  void testLargeNumberOfHitsSameTimestamp() {
    for (int i = 0; i < 100; i++) {
      counter.hit(1);
    }
    assertEquals(100, counter.getHit(1));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 5, 10, 100})
  void testDifferentCapacities(int capacity) {
    ConcurrentHitCounter customCounter = new ConcurrentHitCounter(capacity);
    for (int i = 1; i <= capacity; i++) {
      customCounter.hit(i);
    }
    assertEquals(capacity, customCounter.getHit(capacity));
  }

  @ParameterizedTest
  @CsvSource({"1, 1, 1", "2, 2, 2", "3, 3, 3", "5, 5, 5", "3, 7, 1", "5, 10, 0"})
  void testHitsAndGets(int numHits, int timestamp, int expected) {
    for (int i = 1; i <= numHits; i++) {
      counter.hit(i);
    }
    assertEquals(expected, counter.getHit(timestamp));
  }

  @Test
  void testBoundaryExpiration() {
    counter.hit(1);
    assertEquals(1, counter.getHit(5));
    assertEquals(0, counter.getHit(6));
    assertEquals(0, counter.getHit(7));
  }

  @Test
  void testCapacityOne() {
    ConcurrentHitCounter singleCounter = new ConcurrentHitCounter(1);
    singleCounter.hit(1);
    assertEquals(1, singleCounter.getHit(1));

    singleCounter.hit(2);
    assertEquals(1, singleCounter.getHit(2));

    assertEquals(0, singleCounter.getHit(3));
  }

  @Test
  void testWrapAroundIndices() {
    for (int i = 0; i < CAPACITY; i++) {
      counter.hit(i);
    }
    assertEquals(CAPACITY, counter.getHit(CAPACITY - 1));

    counter.hit(CAPACITY);
    counter.hit(CAPACITY + 1);

    assertEquals(CAPACITY, counter.getHit(CAPACITY + 1));
  }

  @Test
  void testZeroTimestamp() {
    counter.hit(0);
    assertEquals(1, counter.getHit(0));

    counter.hit(0);
    counter.hit(0);
    assertEquals(3, counter.getHit(0));
  }

  @Test
  void testSparseHits() {
    counter.hit(1);
    counter.hit(10);
    counter.hit(20);
    assertEquals(1, counter.getHit(20));
  }

  @Test
  void testGetHitWithNullEntriesInArray() {
    counter.hit(1);
    counter.hit(3);
    assertEquals(0, counter.getHit(10));
  }

  @Test
  void testMultipleExpirationsAtOnce() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    assertEquals(3, counter.getHit(3));

    assertEquals(0, counter.getHit(10));
  }

  @Test
  void testRepeatedHitsWithExpiration() {
    counter.hit(1);
    counter.hit(1);
    counter.hit(1);
    assertEquals(3, counter.getHit(1));

    assertEquals(0, counter.getHit(10));
  }

  @Test
  void testCollisionWithMultipleHitsSameTimestamp() {
    counter.hit(1);
    counter.hit(1);
    counter.hit(1);
    assertEquals(3, counter.getHit(1));

    counter.hit(6);
    assertEquals(1, counter.getHit(6));
  }

  @Test
  void testSequentialHitsAndGets() {
    for (int ts = 1; ts <= 10; ts++) {
      counter.hit(ts);
      int expectedCount = Math.min(ts, CAPACITY);
      assertEquals(expectedCount, counter.getHit(ts));
    }
  }

  @Test
  void testGetHitWithoutAnyHits() {
    assertEquals(0, counter.getHit(0));
    assertEquals(0, counter.getHit(1));
    assertEquals(0, counter.getHit(100));
  }

  @Test
  void testLargeCapacityCounter() {
    ConcurrentHitCounter largeCounter = new ConcurrentHitCounter(1000);
    for (int i = 1; i <= 500; i++) {
      largeCounter.hit(i);
    }
    assertEquals(500, largeCounter.getHit(500));

    assertEquals(0, largeCounter.getHit(1501));
  }

  @Test
  void testExpirationAtExactBoundary() {
    counter.hit(10);
    counter.hit(11);
    counter.hit(12);

    assertEquals(3, counter.getHit(14));
    assertEquals(2, counter.getHit(15));
    assertEquals(1, counter.getHit(16));
    assertEquals(0, counter.getHit(17));
  }

  @Test
  void testMixedOperations() {
    counter.hit(1);
    assertEquals(1, counter.getHit(1));

    counter.hit(2);
    counter.hit(3);
    assertEquals(3, counter.getHit(3));

    counter.hit(6);
    assertEquals(3, counter.getHit(6));

    counter.hit(11);
    assertEquals(1, counter.getHit(11));
  }

  @Test
  void testConcurrentHitsSameTimestamp() throws InterruptedException {
    int numThreads = 10;
    int hitsPerThread = 100;
    int timestamp = 1;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < hitsPerThread; j++) {
              counter.hit(timestamp);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(numThreads * hitsPerThread, counter.getHit(timestamp));
  }

  @Test
  void testConcurrentHitsDifferentTimestamps() throws InterruptedException {
    int numThreads = 5;
    int hitsPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            for (int j = 0; j < hitsPerThread; j++) {
              counter.hit(threadId);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    int totalHits = counter.getHit(numThreads - 1);
    assertEquals(numThreads * hitsPerThread, totalHits);
  }

  @Test
  void testConcurrentHitsAndGets() throws InterruptedException {
    int numHitThreads = 5;
    int numGetThreads = 5;
    int hitsPerThread = 100;
    int timestamp = 5;

    ExecutorService executor = Executors.newFixedThreadPool(numHitThreads + numGetThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numHitThreads + numGetThreads);

    for (int i = 0; i < numHitThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < hitsPerThread; j++) {
                counter.hit(timestamp);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    for (int i = 0; i < numGetThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < hitsPerThread; j++) {
                int hits = counter.getHit(timestamp);
                assertTrue(hits >= 0 && hits <= numHitThreads * hitsPerThread);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    endLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(numHitThreads * hitsPerThread, counter.getHit(timestamp));
  }

  @Test
  void testConcurrentCollisions() throws InterruptedException {
    int numThreads = 10;
    int hitsPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            for (int j = 0; j < hitsPerThread; j++) {
              int timestamp = threadId * CAPACITY;
              counter.hit(timestamp);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    // Every timestamp i*CAPACITY maps to the same bucket (timestamp % CAPACITY == 0), so all
    // threads contend for a single slot. With distinct timestamps the slot is overwritten rather
    // than accumulated, so exactly one thread's run survives — which one depends on scheduling and
    // is not deterministic. The invariant that always holds is that the surviving count is one
    // thread's uncorrupted run: at least one hit, at most hitsPerThread. Query at timestamp 0 so
    // the surviving slot (timestamp >= 0) is never lazily expired regardless of the winner.
    int surviving = counter.getHit(0);
    assertTrue(
        surviving >= 1 && surviving <= hitsPerThread,
        "colliding timestamps overwrite one slot; the survivor must be a single uncorrupted run but was "
            + surviving);
  }

  @RepeatedTest(10)
  void testConcurrentUpdatesSameIndex() throws InterruptedException {
    int numThreads = 20;
    int hitsPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < hitsPerThread; j++) {
              counter.hit(1);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(numThreads * hitsPerThread, counter.getHit(1));
  }

  @Test
  void testConcurrentExpirations() throws InterruptedException {
    for (int i = 1; i <= 5; i++) {
      counter.hit(i);
    }

    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    List<Integer> results = new CopyOnWriteArrayList<>();

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            int hits = counter.getHit(10);
            results.add(hits);
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    for (int result : results) {
      assertTrue(result >= 0 && result <= 5);
    }

    assertEquals(0, counter.getHit(10));
  }

  @Test
  void testHighConcurrencyStressTest() throws InterruptedException {
    int numThreads = 50;
    int operationsPerThread = 1000;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger hitCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            for (int j = 0; j < operationsPerThread; j++) {
              if (j % 2 == 0) {
                counter.hit(threadId % 10);
                hitCount.incrementAndGet();
              } else {
                counter.getHit(threadId % 10);
              }
            }
            latch.countDown();
          });
    }

    latch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
  }

  @Test
  void testConcurrentMixedTimestamps() throws InterruptedException {
    int numThreads = 10;
    int operationsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      final int threadId = i;
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < operationsPerThread; j++) {
                int timestamp = (threadId * operationsPerThread + j) % 20;
                counter.hit(timestamp);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    endLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    int totalHits = counter.getHit(1000);
    assertTrue(totalHits >= 0);
  }

  @Test
  void testAtomicityOfTotalHitsCounter() throws InterruptedException {
    int numThreads = 20;
    int hitsPerThread = 100;
    int timestamp = 42;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < hitsPerThread; j++) {
              counter.hit(timestamp);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    int finalCount = counter.getHit(timestamp);
    assertEquals(numThreads * hitsPerThread, finalCount);
  }

  @Test
  void testNoLostUpdatesUnderConcurrency() throws InterruptedException {
    int numThreads = 15;
    int[] timestamps = {1, 2, 3, 4, 5};
    int hitsPerTimestampPerThread = 20;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int timestamp : timestamps) {
              for (int j = 0; j < hitsPerTimestampPerThread; j++) {
                counter.hit(timestamp);
              }
            }
            latch.countDown();
          });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    int expectedTotal = timestamps.length * numThreads * hitsPerTimestampPerThread;
    assertEquals(expectedTotal, counter.getHit(5));
  }

  @Test
  void testConcurrentGetDoesNotCorruptState() throws InterruptedException {
    for (int i = 1; i <= 5; i++) {
      counter.hit(i);
    }

    int numThreads = 20;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              counter.getHit(5);
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(5, counter.getHit(5));
  }

  @Test
  void testRaceConditionOnSameSlot() throws InterruptedException {
    int numThreads = 10;
    int timestamp1 = 100;
    int timestamp2 = 105;
    int hitsPerThread = 50;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numThreads);

    for (int i = 0; i < numThreads / 2; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < hitsPerThread; j++) {
                counter.hit(timestamp1);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    for (int i = 0; i < numThreads / 2; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < hitsPerThread; j++) {
                counter.hit(timestamp2);
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              endLatch.countDown();
            }
          });
    }

    startLatch.countDown();
    endLatch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    int totalHits = counter.getHit(timestamp2);
    assertTrue(totalHits > 0, "Expected some hits but was " + totalHits);
  }
}
