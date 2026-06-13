package dev.aahmedlab.ratelimiter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SlidingWindowCounterTest {

  private SlidingWindowCounter limiter;

  @BeforeEach
  void setUp() {
    limiter = new SlidingWindowCounter(10, 1000);
  }

  @Test
  void testConstructorRejectsInvalidCapacity() {
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowCounter(0, 1000));
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowCounter(-1, 1000));
  }

  @Test
  void testConstructorRejectsInvalidWindow() {
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowCounter(10, 0));
    assertThrows(IllegalArgumentException.class, () -> new SlidingWindowCounter(10, -1));
  }

  @Test
  void testInitialState() {
    assertTrue(limiter.allowRequest());
  }

  @Test
  void testConsumeAllCapacity() {
    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.allowRequest());
    }
    assertFalse(limiter.allowRequest());
  }

  @Test
  void testCapacityOne() {
    SlidingWindowCounter single = new SlidingWindowCounter(1, 1000);
    assertTrue(single.allowRequest());
    assertFalse(single.allowRequest());
  }

  @Test
  void testFullResetAfterMultipleWindows() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.allowRequest());
    }
    assertFalse(limiter.allowRequest());

    Thread.sleep(2100);

    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.allowRequest());
    }
    assertFalse(limiter.allowRequest());
  }

  @Test
  void testPreviousWindowWeightingLimitsBurst() throws InterruptedException {
    for (int i = 0; i < 10; i++) {
      assertTrue(limiter.allowRequest());
    }
    assertFalse(limiter.allowRequest());

    Thread.sleep(1100);

    int allowed = 0;
    for (int i = 0; i < 10; i++) {
      if (limiter.allowRequest()) {
        allowed++;
      }
    }
    assertTrue(allowed < 10, "Weighted previous window should restrict the new window's allowance");
    assertTrue(allowed >= 0);
  }

  @Test
  void testConcurrentRequestsNeverExceedCapacity() throws InterruptedException {
    int numThreads = 20;
    int requestsPerThread = 10;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (limiter.allowRequest()) {
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
    SlidingWindowCounter l = new SlidingWindowCounter(10, 1000);
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
                if (l.allowRequest()) {
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
  void testNoLostUpdates() throws InterruptedException {
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
              if (limiter.allowRequest()) {
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
    assertTrue(allowedCount.get() <= 10);
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 5, 10, 50, 100})
  void testConcurrentVariousCapacities(int capacity) throws InterruptedException {
    SlidingWindowCounter l = new SlidingWindowCounter(capacity, 1000);
    int numThreads = 10;
    int requestsPerThread = capacity;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch latch = new CountDownLatch(numThreads);
    AtomicInteger allowedCount = new AtomicInteger(0);

    for (int i = 0; i < numThreads; i++) {
      executor.submit(
          () -> {
            for (int j = 0; j < requestsPerThread; j++) {
              if (l.allowRequest()) {
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
}
