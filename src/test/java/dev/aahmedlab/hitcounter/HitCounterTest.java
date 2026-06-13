package dev.aahmedlab.hitcounter;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class HitCounterTest {

  private static final int CAPACITY = 5;
  private HitCounter counter;

  @BeforeEach
  void setUp() {
    counter = new HitCounter(CAPACITY);
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
    counter.hit(0); // index 0 % 5 = 0
    counter.hit(1); // index 1 % 5 = 1
    counter.hit(5); // index 5 % 5 = 0 (collides with timestamp 0, removes it)
    assertEquals(2, counter.getHit(5)); // only ts 1 and ts 5 remain
  }

  @Test
  void testCollisionReplacesOldTimestamp() {
    counter.hit(1); // index 1
    counter.hit(6); // index 6 % 5 = 1, replaces timestamp 1
    assertEquals(1, counter.getHit(6)); // only count from timestamp 6
  }

  @Test
  void testCollisionAdjustsTotalHits() {
    counter.hit(1);
    counter.hit(2);
    assertEquals(2, counter.getHit(2));

    counter.hit(6); // collides with index 1 (timestamp 1), removes that count
    assertEquals(2, counter.getHit(6)); // 2 (from ts 2) + 1 (new ts 6) - 1 (removed ts 1) = 2
  }

  @Test
  void testOldHitsAreExpired() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    assertEquals(3, counter.getHit(3));

    // timestamp 1 is expired at ts 6 (6 - 1 = 5 >= 5)
    assertEquals(2, counter.getHit(6)); // ts 1 is expired, ts 2 and 3 remain
  }

  @Test
  void testGetHitCleansExpiredEntries() {
    counter.hit(1);
    counter.hit(2);
    assertEquals(2, counter.getHit(2));

    // Expire hits at timestamp 6: ts 1 expired (6-1=5>=5)
    assertEquals(1, counter.getHit(6)); // ts 1 expired, only ts 2 remains
    assertEquals(0, counter.getHit(7)); // both expired (7-2=5>=5)
  }

  @Test
  void testGetHitCleansExpiredEntriesAndCountsCorrectly() {
    counter.hit(1);
    counter.hit(2);
    counter.hit(3);
    counter.hit(4);
    counter.hit(5);
    assertEquals(5, counter.getHit(5));

    // At timestamp 6, hits with ts 1 are expired (6 - 1 = 5 >= 5)
    assertEquals(4, counter.getHit(6));

    // At timestamp 7, hits with ts 1 and 2 are expired
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
    HitCounter customCounter = new HitCounter(capacity);
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
    // With capacity 5, a hit at ts=1 expires when current ts >= 6
    counter.hit(1);
    assertEquals(1, counter.getHit(5)); // 5 - 1 = 4 < 5, not expired
    assertEquals(0, counter.getHit(6)); // 6 - 1 = 5 >= 5, expired and cleaned
    assertEquals(0, counter.getHit(7)); // already cleaned
  }

  @Test
  void testCapacityOne() {
    HitCounter singleCounter = new HitCounter(1);
    singleCounter.hit(1);
    assertEquals(1, singleCounter.getHit(1));

    singleCounter.hit(2); // replaces ts 1
    assertEquals(1, singleCounter.getHit(2));

    // At ts 3, ts 2 should be expired (3 - 2 = 1 >= 1)
    assertEquals(0, singleCounter.getHit(3));
  }

  @Test
  void testWrapAroundIndices() {
    // Fill the array
    for (int i = 0; i < CAPACITY; i++) {
      counter.hit(i);
    }
    assertEquals(CAPACITY, counter.getHit(CAPACITY - 1));

    // Add more hits that wrap around
    counter.hit(CAPACITY); // index 0
    counter.hit(CAPACITY + 1); // index 1

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
    counter.hit(1); // index 1
    counter.hit(10); // index 0
    counter.hit(20); // index 0 (collides with ts 10, removes it)
    assertEquals(1, counter.getHit(20)); // only ts 20 remains (ts 1 expired: 20-1=19>=5)
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
    HitCounter largeCounter = new HitCounter(1000);
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

    assertEquals(3, counter.getHit(14)); // 14-10=4<5, all remain
    assertEquals(2, counter.getHit(15)); // 15-10=5>=5, ts 10 expired
    assertEquals(1, counter.getHit(16)); // 16-11=5>=5, ts 11 expired
    assertEquals(0, counter.getHit(17)); // 17-12=5>=5, ts 12 expired
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
}
