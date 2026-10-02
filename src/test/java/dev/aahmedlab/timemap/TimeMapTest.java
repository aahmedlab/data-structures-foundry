package dev.aahmedlab.timemap;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TimeMapTest {

  private TimeMap timeMap;

  @BeforeEach
  void setUp() {
    timeMap = new TimeMap();
  }

  @Test
  void testGetMissingKeyReturnsEmptyString() {
    assertEquals("", timeMap.get("absent", 1));
  }

  @Test
  void testGetAtExactTimestamp() {
    timeMap.set("foo", "bar", 1);

    assertEquals("bar", timeMap.get("foo", 1));
  }

  @Test
  void testGetBeforeFirstTimestampReturnsEmptyString() {
    timeMap.set("foo", "bar", 5);

    assertEquals("", timeMap.get("foo", 4));
    assertEquals("", timeMap.get("foo", 0));
  }

  @Test
  void testGetAfterLastTimestampReturnsLatestValue() {
    timeMap.set("foo", "bar", 1);

    assertEquals("bar", timeMap.get("foo", 100));
  }

  @Test
  void testLeetCodeExample() {
    timeMap.set("foo", "bar", 1);
    assertEquals("bar", timeMap.get("foo", 1));
    assertEquals("bar", timeMap.get("foo", 3));

    timeMap.set("foo", "bar2", 4);
    assertEquals("bar2", timeMap.get("foo", 4));
    assertEquals("bar2", timeMap.get("foo", 5));
  }

  @ParameterizedTest(name = "get(t={0}) -> \"{1}\"")
  @CsvSource({
    "0,  ''",
    "9,  ''",
    "10, v10",
    "15, v10",
    "19, v10",
    "20, v20",
    "29, v20",
    "30, v30",
    "39, v30",
    "40, v40",
    "50, v50",
    "999, v50",
  })
  void testGetReturnsLatestValueAtOrBeforeTimestamp(int queryTime, String expected) {
    for (int t = 10; t <= 50; t += 10) {
      timeMap.set("k", "v" + t, t);
    }

    assertEquals(expected, timeMap.get("k", queryTime));
  }

  @Test
  void testKeysAreIndependent() {
    timeMap.set("a", "a1", 1);
    timeMap.set("b", "b5", 5);
    timeMap.set("a", "a10", 10);

    assertEquals("a1", timeMap.get("a", 5));
    assertEquals("", timeMap.get("b", 4));
    assertEquals("b5", timeMap.get("b", 10));
    assertEquals("a10", timeMap.get("a", 10));
  }

  @Test
  void testSameTimestampSetTwiceReturnsLaterValue() {
    timeMap.set("k", "first", 3);
    timeMap.set("k", "second", 3);

    assertEquals("second", timeMap.get("k", 3));
    assertEquals("second", timeMap.get("k", 4));
  }

  @Test
  void testEmptyStringValueIsStored() {
    timeMap.set("k", "real", 1);
    timeMap.set("k", "", 2);

    assertEquals("", timeMap.get("k", 2));
    assertEquals("real", timeMap.get("k", 1));
  }

  @Test
  void testNegativeQueryTimeReturnsEmptyString() {
    timeMap.set("k", "v", 0);

    assertEquals("", timeMap.get("k", -1));
    assertEquals("v", timeMap.get("k", 0));
  }

  @Test
  void testLargeTimestamps() {
    timeMap.set("k", "max-1", Integer.MAX_VALUE - 1);
    timeMap.set("k", "max", Integer.MAX_VALUE);

    assertEquals("max-1", timeMap.get("k", Integer.MAX_VALUE - 1));
    assertEquals("max", timeMap.get("k", Integer.MAX_VALUE));
  }

  @Test
  void testEveryQueryOverManyEntries() {
    // Even timestamps 0..1998; odd queries fall between entries.
    for (int t = 0; t < 2000; t += 2) {
      timeMap.set("k", "v" + t, t);
    }

    for (int q = 0; q < 2001; q++) {
      int expected = Math.min(q - (q % 2), 1998);
      assertEquals("v" + expected, timeMap.get("k", q), "query " + q);
    }
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 42L, 2024L})
  void testRandomOperationsMatchTreeMapFloor(long seed) {
    Random rnd = new Random(seed);
    String[] keys = {"a", "b", "c", "d"};
    Map<String, TreeMap<Integer, String>> reference = new HashMap<>();
    Map<String, Integer> lastTime = new HashMap<>();

    for (int i = 0; i < 3000; i++) {
      String key = keys[rnd.nextInt(keys.length)];
      if (rnd.nextBoolean()) {
        // Timestamps per key are strictly increasing, as the binary search requires.
        int t = lastTime.getOrDefault(key, 0) + 1 + rnd.nextInt(5);
        lastTime.put(key, t);
        String value = key + "@" + t;
        timeMap.set(key, value, t);
        reference.computeIfAbsent(key, k -> new TreeMap<>()).put(t, value);
      } else {
        int q = rnd.nextInt(lastTime.getOrDefault(key, 0) + 10);
        TreeMap<Integer, String> history = reference.get(key);
        Map.Entry<Integer, String> floor = history == null ? null : history.floorEntry(q);
        assertEquals(floor == null ? "" : floor.getValue(), timeMap.get(key, q));
      }
    }
  }
}
