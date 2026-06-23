package dev.aahmedlab.ratelimiter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class LockFreeTokenBucketTest {

  @Test
  void testRejectsInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> new LockFreeTokenBucket(0, 1));
    assertThrows(IllegalArgumentException.class, () -> new LockFreeTokenBucket(1, 0));
    assertThrows(IllegalArgumentException.class, () -> new LockFreeTokenBucket(-1, -1));
  }

  @Test
  void testAllowsUpToCapacityThenDenies() {
    // refillRate 1 => one token per 1000 ms, so no refill happens during this fast test.
    LockFreeTokenBucket bucket = new LockFreeTokenBucket(3, 1);

    assertTrue(bucket.allowRequest());
    assertTrue(bucket.allowRequest());
    assertTrue(bucket.allowRequest());
    assertFalse(bucket.allowRequest(), "Fourth request exceeds capacity and is denied");
  }

  @Test
  void testRefillsOverTime() throws InterruptedException {
    // refillRate 1000 => one token per millisecond, so a short pause restores tokens.
    LockFreeTokenBucket bucket = new LockFreeTokenBucket(2, 1000);
    assertTrue(bucket.allowRequest());
    assertTrue(bucket.allowRequest());
    assertFalse(bucket.allowRequest(), "Bucket drained");

    Thread.sleep(50);

    assertTrue(bucket.allowRequest(), "Tokens should have refilled after the pause");
  }

  @RepeatedTest(10)
  void testNeverGrantsMoreThanCapacityUnderContention() throws InterruptedException {
    int capacity = 100;
    int numThreads = 16;
    LockFreeTokenBucket bucket = new LockFreeTokenBucket(capacity, 1); // no refill during the test

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(numThreads);
    AtomicInteger granted = new AtomicInteger();

    for (int t = 0; t < numThreads; t++) {
      executor.submit(
          () -> {
            try {
              start.await();
              for (int i = 0; i < capacity; i++) {
                if (bucket.allowRequest()) granted.incrementAndGet();
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

    assertEquals(capacity, granted.get(), "CAS must grant exactly capacity tokens, never more");
  }
}
