package dev.aahmedlab.map;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChainingHashMapTest {

  @Test
  void testPutAndGet() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    map.put("a", 1);
    map.put("b", 2);

    assertEquals(1, map.get("a"));
    assertEquals(2, map.get("b"));
  }

  @Test
  void testGetMissingKeyReturnsNull() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    map.put("a", 1);

    assertNull(map.get("absent"));
  }

  @Test
  void testPutUpdatesExistingKey() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    map.put("a", 1);
    map.put("a", 99);

    assertEquals(99, map.get("a"), "Re-putting a key should overwrite its value");
  }

  @Test
  void testRemoveReturnsOldValueAndDeletes() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    map.put("a", 1);

    assertEquals(1, map.remove("a"), "remove should return the previous value");
    assertNull(map.get("a"), "Key should be gone after remove");
  }

  @Test
  void testRemoveMissingKeyReturnsNull() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    assertNull(map.remove("absent"));
  }

  @Test
  void testCollidingKeysShareSlotAndCoexist() {
    // With Integer keys < 65536, spread(i) == i, so index == i % 64. Keys 1 and 65 collide
    // in the initial 64-slot table, exercising chained insertion and lookup.
    ChainingHashMap<Integer, String> map = new ChainingHashMap<>();
    map.put(1, "one");
    map.put(65, "sixty-five");

    assertEquals("one", map.get(1));
    assertEquals("sixty-five", map.get(65));
  }

  @Test
  void testRemoveHeadOfChain() {
    ChainingHashMap<Integer, String> map = new ChainingHashMap<>();
    map.put(1, "one"); // chain: [1]
    map.put(65, "sixty-five"); // chain head is now 65 -> 1

    assertEquals("sixty-five", map.remove(65), "Removing the chain head (prev == null branch)");
    assertNull(map.get(65));
    assertEquals("one", map.get(1), "Tail of the chain must survive head removal");
  }

  @Test
  void testRemoveTailOfChain() {
    ChainingHashMap<Integer, String> map = new ChainingHashMap<>();
    map.put(1, "one"); // chain: [1]
    map.put(65, "sixty-five"); // chain head is now 65 -> 1

    assertEquals("one", map.remove(1), "Removing a non-head node (prev != null branch)");
    assertNull(map.get(1));
    assertEquals("sixty-five", map.get(65), "Head of the chain must survive tail removal");
  }

  @Test
  void testResizePreservesAllEntries() {
    // Threshold is 64 * 0.75 == 48, so inserting well past it forces a resize to 128 slots.
    ChainingHashMap<Integer, Integer> map = new ChainingHashMap<>();
    int n = 200;
    for (int i = 0; i < n; i++) {
      map.put(i, i * 10);
    }

    for (int i = 0; i < n; i++) {
      assertEquals(i * 10, map.get(i), "Every entry must survive rehashing during resize");
    }
  }

  @Test
  void testResizePreservesUpdatesAndRemovals() {
    ChainingHashMap<Integer, Integer> map = new ChainingHashMap<>();
    for (int i = 0; i < 100; i++) {
      map.put(i, i);
    }
    map.put(10, 1000); // update after growth
    assertEquals(1000, map.remove(10), "Updated value survives across resize");

    assertNull(map.get(10));
    assertEquals(50, map.get(50), "Unrelated entries remain intact");
  }

  @Test
  void testSizeTracksInsertsButNotUpdates() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    assertEquals(0, map.getSize(), "Fresh map is empty");

    map.put("a", 1);
    map.put("b", 2);
    assertEquals(2, map.getSize(), "Each new key bumps size");

    map.put("a", 99); // update, not insert
    assertEquals(2, map.getSize(), "Overwriting an existing key must not grow size");
    assertEquals(99, map.get("a"), "Update still took effect");
  }

  @Test
  void testReinsertAfterRemoveRestoresSizeAndChain() {
    // Keys 1 and 65 share a slot, so this also exercises chain relinking, not just a lone bucket.
    ChainingHashMap<Integer, String> map = new ChainingHashMap<>();
    map.put(1, "one"); // chain: 1
    map.put(65, "sixty-five"); // chain: 65 -> 1
    assertEquals(2, map.getSize());

    assertEquals("one", map.remove(1));
    assertEquals(1, map.getSize(), "remove decrements size");
    assertNull(map.get(1));

    map.put(1, "one-again"); // re-insert the removed key
    assertEquals(2, map.getSize(), "Re-inserting a removed key bumps size back up");
    assertEquals("one-again", map.get(1), "Re-inserted value is readable");
    assertEquals("sixty-five", map.get(65), "Chain neighbor is undisturbed by the churn");
  }

  @Test
  void testNullKeyThrows() {
    ChainingHashMap<String, Integer> map = new ChainingHashMap<>();
    assertThrows(NullPointerException.class, () -> map.put(null, 1));
    assertThrows(NullPointerException.class, () -> map.get(null));
    assertThrows(NullPointerException.class, () -> map.remove(null));
  }

  @Test
  void testRemoveMiddleOfChain() {
    ChainingHashMap<Integer, String> map = new ChainingHashMap<>();
    map.put(1, "one"); // chain: 1
    map.put(65, "sixty-five"); // chain: 65 -> 1
    map.put(129, "one-two-nine"); // chain: 129 -> 65 -> 1

    assertEquals("sixty-five", map.remove(65), "removing a middle node (non-null successor)");
    assertNull(map.get(65));
    assertEquals("one", map.get(1), "node AFTER the removed one must survive");
    assertEquals("one-two-nine", map.get(129), "node BEFORE the removed one must survive");
  }
}
