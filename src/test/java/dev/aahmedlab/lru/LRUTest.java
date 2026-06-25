package dev.aahmedlab.lru;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LRUTest {

    @Test
    void testBasicGetPut() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);

        assertEquals(10, lru.get(1), "Get existing key should return value");
        assertEquals(20, lru.get(2), "Get existing key should return value");
        assertEquals(-1, lru.get(3), "Get non-existent key should return -1");
    }

    @Test
    void testEviction() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(3, 30); // Should evict key 1

        assertEquals(-1, lru.get(1), "Evicted key should not be retrievable");
        assertEquals(20, lru.get(2), "Key 2 should still exist");
        assertEquals(30, lru.get(3), "Key 3 should exist");
        assertEquals(2, lru.size(), "Size should be exactly capacity");
    }

    @Test
    void testUpdateExisting() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(1, 100); // Update key 1, moves to head
        lru.put(3, 30); // Should evict key 2

        assertEquals(100, lru.get(1), "Updated value should be retrieved");
        assertEquals(-1, lru.get(2), "Key 2 should be evicted");
        assertEquals(30, lru.get(3), "Key 3 should exist");
    }

    @Test
    void testCapacityOne() {
        LRU lru = new LRU(1);
        lru.put(1, 10);
        assertEquals(10, lru.get(1), "Single capacity should work");

        lru.put(2, 20);
        assertEquals(-1, lru.get(1), "Key 1 should be evicted");
        assertEquals(20, lru.get(2), "Key 2 should exist");
        assertEquals(1, lru.size(), "Size should remain at capacity");
    }

    @Test
    void testAccessOrder() {
        LRU lru = new LRU(3);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(3, 30);

        lru.get(1); // Access key 1, move to head
        lru.put(4, 40); // Should evict key 2

        assertEquals(10, lru.get(1), "Recently accessed key 1 should exist");
        assertEquals(-1, lru.get(2), "Key 2 should be evicted");
        assertEquals(30, lru.get(3), "Key 3 should exist");
        assertEquals(40, lru.get(4), "Key 4 should exist");
    }

    @Test
    void testLargeCapacity() {
        LRU lru = new LRU(100);
        for (int i = 0; i < 100; i++) {
            lru.put(i, i * 10);
        }

        assertEquals(100, lru.size(), "Should hold exactly capacity items");

        lru.put(100, 1000);
        assertEquals(-1, lru.get(0), "First inserted key should be evicted");
        assertEquals(990, lru.get(99), "Last inserted key should exist");
        assertEquals(100, lru.size(), "Size should stay at capacity");
    }

    @Test
    void testInvalidCapacity() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> new LRU(0));
        assertTrue(exception.getMessage().contains("Capacity must be > 0"));
    }

    @Test
    void testClear() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);

        lru.clear();
        assertEquals(0, lru.size(), "After clear, size should be 0");
        assertEquals(-1, lru.get(1), "After clear, keys should not exist");
        assertFalse(lru.containsKey(1), "containsKey should return false");

        lru.put(3, 30); // Should work fine after clear
        assertEquals(30, lru.get(3), "Should accept new inserts after clear");
    }

    @Test
    void testContainsKey() {
        LRU lru = new LRU(2);
        lru.put(1, 10);

        assertTrue(lru.containsKey(1), "Should contain key 1");
        assertFalse(lru.containsKey(2), "Should not contain key 2");
    }
}
