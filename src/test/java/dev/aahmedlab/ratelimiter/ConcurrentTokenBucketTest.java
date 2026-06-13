package dev.aahmedlab.ratelimiter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ConcurrentTokenBucketTest {

  private ConcurrentTokenBucket tokenBucket;

  @BeforeEach
  void setUp() {
    tokenBucket = new ConcurrentTokenBucket(10, 5);
  }

  @Test
  void testInitialState() {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(10, 5);
    assertTrue(bucket.allowRequest());
  }

  @Test
  void testConsumeAllTokens() {
    for (int i = 0; i < 10; i++) {
      assertTrue(tokenBucket.allowRequest());
    }
    assertFalse(tokenBucket.allowRequest());
  }

  @Test
  void testRefillOverTime() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      assertTrue(tokenBucket.allowRequest());
    }
    assertFalse(tokenBucket.allowRequest());

    Thread.sleep(1000);

    for (int i = 0; i < 5; i++) {
      assertTrue(tokenBucket.allowRequest());
    }
    assertFalse(tokenBucket.allowRequest());
  }

  @Test
  void testBucketNeverExceedsCapacity() throws InterruptedException {
    for (int i = 0; i < 5; i++) {
      assertTrue(tokenBucket.allowRequest());
    }

    Thread.sleep(3000);

    for (int i = 0; i < 10; i++) {
      assertTrue(tokenBucket.allowRequest());
    }
    assertFalse(tokenBucket.allowRequest());
  }

  @Test
  void testZeroRefillRate() {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(5, 0);
    for (int i = 0; i < 5; i++) {
      assertTrue(bucket.allowRequest());
    }
    assertFalse(bucket.allowRequest());
  }

  @Test
  void testCapacityOne() {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(1, 1);
    assertTrue(bucket.allowRequest());
    assertFalse(bucket.allowRequest());
  }

  @ParameterizedTest
  @CsvSource({"5, 1, 5", "10, 5, 10", "20, 10, 20", "100, 50, 100"})
  void testDifferentCapacitiesAndRefillRates(
      int capacity, int refillRate, int expectedInitialTokens) {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(capacity, refillRate);
    int allowedCount = 0;
    for (int i = 0; i < expectedInitialTokens + 1; i++) {
      if (bucket.allowRequest()) {
        allowedCount++;
      }
    }
    assertEquals(expectedInitialTokens, allowedCount);
  }

  @Test
  void testConcurrentRequests() throws InterruptedException {
    int numThreads = 10;
    int requestsPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(10, allowedCount.get());
  }

  @Test
  void testConcurrentRequestsExceedingCapacity() throws InterruptedException {
    int numThreads = 20;
    int requestsPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(10, allowedCount.get());
  }

  @RepeatedTest(10)
  void testConcurrentRequestsSameTime() throws InterruptedException {
    int numThreads = 15;
    int requestsPerThread = 5;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < requestsPerThread; j++) {
                if (tokenBucket.allowRequest()) {
                  allowedCount.incrementAndGet();
                }
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

    assertEquals(10, allowedCount.get());
  }

  @Test
  void testConcurrentRequestsWithRefill() throws InterruptedException {
    int numThreads = 5;
    int requestsPerThread = 20;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
              try {
                Thread.sleep(50);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            latch.countDown();
          });
    }

    latch.await(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(allowedCount.get() > 10, "Should have more than initial capacity due to refill");
  }

  @Test
  void testNoRaceConditionOnBucketState() throws InterruptedException {
    int numThreads = 20;
    int requestsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);
    AtomicInteger deniedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              } else {
                deniedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(numThreads * requestsPerThread, allowedCount.get() + deniedCount.get());
    assertTrue(allowedCount.get() >= 10, "At least capacity requests should be allowed");
  }

  @Test
  void testConcurrentEmptyBucketBehavior() throws InterruptedException {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(5, 1);

    for (int i = 0; i < 5; i++) {
      assertTrue(bucket.allowRequest());
    }

    int numThreads = 10;
    int requestsPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (bucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(0, allowedCount.get());
  }

  @Test
  void testHighConcurrencyStressTest() throws InterruptedException {
    int numThreads = 50;
    int requestsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(30, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(10, allowedCount.get());
  }

  @Test
  void testConcurrentRefillAndConsumption() throws InterruptedException {
    int consumerThreads = 5;
    int requestsPerConsumer = 50;

    ExecutorService executor = Executors.newFixedThreadPool(consumerThreads);
    CountDownLatch latch = new CountDownLatch(consumerThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < consumerThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerConsumer; j++) {
              if (tokenBucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
              try {
                Thread.sleep(100);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            latch.countDown();
          });
    }

    latch.await(20, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(allowedCount.get() > 10, "Should have more than initial capacity due to refill");
  }

  @Test
  void testConcurrentPartialRefill() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      assertTrue(tokenBucket.allowRequest());
    }
    assertFalse(tokenBucket.allowRequest());

    Thread.sleep(200);

    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            if (tokenBucket.allowRequest()) {
              allowedCount.incrementAndGet();
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(1, allowedCount.get());
  }

  @Test
  void testConcurrentMultipleRefillCycles() throws InterruptedException {
    int numThreads = 3;
    int cycles = 3;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int cycle = 0; cycle < cycles; cycle++) {
              for (int j = 0; j < 10; j++) {
                if (tokenBucket.allowRequest()) {
                  allowedCount.incrementAndGet();
                }
              }
              try {
                // At 5 tokens/sec, a full capacity (10) refill needs ~2s between cycles.
                Thread.sleep(2100);
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            }
            latch.countDown();
          });
    }

    latch.await(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertTrue(allowedCount.get() >= 30, "Should have at least 3 cycles * 10 tokens");
  }

  @Test
  void testAtomicityOfTokenConsumption() throws InterruptedException {
    int numThreads = 20;
    int requestsPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            try {
              startLatch.await();
              for (int j = 0; j < requestsPerThread; j++) {
                if (tokenBucket.allowRequest()) {
                  allowedCount.incrementAndGet();
                }
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

    assertEquals(10, allowedCount.get());
  }

  @Test
  void testConcurrentFractionalTokenAccumulation() throws InterruptedException {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(10, 1);

    for (int i = 0; i < 10; i++) {
      assertTrue(bucket.allowRequest());
    }
    assertFalse(bucket.allowRequest());

    Thread.sleep(500);

    int numThreads = 5;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            if (bucket.allowRequest()) {
              allowedCount.incrementAndGet();
            }
            latch.countDown();
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(0, allowedCount.get());
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 50, 100})
  void testConcurrentVariousCapacities(int capacity) throws InterruptedException {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(capacity, capacity);
    int numThreads = 10;
    int requestsPerThread = capacity / 2 + 5;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (bucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(10, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(capacity, allowedCount.get());
  }

  @Test
  void testConcurrentLargeCapacity() throws InterruptedException {
    ConcurrentTokenBucket bucket = new ConcurrentTokenBucket(1000, 100);
    int numThreads = 20;
    int requestsPerThread = 100;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (bucket.allowRequest()) {
                allowedCount.incrementAndGet();
              }
            }
            latch.countDown();
          });
    }

    latch.await(15, TimeUnit.SECONDS);
    executor.shutdown();

    assertEquals(1000, allowedCount.get());
  }
}
