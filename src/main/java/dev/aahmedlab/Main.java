package dev.aahmedlab;

import dev.aahmedlab.lru.LRU;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
/**
 * A standalone smoke-test driver that exercises the {@link LRU} cache from {@code main}.
 * <p>
 * Runs a fixed sequence of scenarios (basic get/put, eviction, update, capacity-one,
 * access order, large capacity, invalid capacity, clear) using {@code assert} statements,
 * printing a check mark per scenario.
 * <p>
 * <b>Design Decisions:</b>
 * <ul>
 *   <li><b>{@code assert}-based checks:</b> Keeps the driver dependency-free, but requires
 *       running the JVM with {@code -ea} (assertions enabled) for failures to surface.</li>
 *   <li><b>Duplicated coverage:</b> These scenarios mirror the JUnit tests in
 *       {@code LRUTest}; this class exists for quick manual runs from the IDE.</li>
 * </ul>
 */
public class Main {
    public static void main(String[] args) {
        testBasicGetPut();
        testEviction();
        testUpdateExisting();
        testCapacityOne();
        testAccessOrder();
        testLargeCapacity();
        testInvalidCapacity();
        testClear();
        System.out.println("✓ All tests passed");
    }

    private static void testBasicGetPut() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);

        assert lru.get(1) == 10 : "Get existing key should return value";
        assert lru.get(2) == 20 : "Get existing key should return value";
        assert lru.get(3) == -1 : "Get non-existent key should return -1";
        System.out.println("✓ testBasicGetPut passed");
    }

    private static void testEviction() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(3, 30); // Should evict key 1

        assert lru.get(1) == -1 : "Evicted key should not be retrievable";
        assert lru.get(2) == 20 : "Key 2 should still exist";
        assert lru.get(3) == 30 : "Key 3 should exist";
        assert lru.size() == 2 : "Size should be exactly capacity";
        System.out.println("✓ testEviction passed");
    }

    private static void testUpdateExisting() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(1, 100); // Update key 1, moves to head
        lru.put(3, 30);  // Should evict key 2

        assert lru.get(1) == 100 : "Updated value should be retrieved";
        assert lru.get(2) == -1 : "Key 2 should be evicted";
        assert lru.get(3) == 30 : "Key 3 should exist";
        System.out.println("✓ testUpdateExisting passed");
    }

    private static void testCapacityOne() {
        LRU lru = new LRU(1);
        lru.put(1, 10);
        assert lru.get(1) == 10 : "Single capacity should work";

        lru.put(2, 20);
        assert lru.get(1) == -1 : "Key 1 should be evicted";
        assert lru.get(2) == 20 : "Key 2 should exist";
        assert lru.size() == 1 : "Size should remain at capacity";
        System.out.println("✓ testCapacityOne passed");
    }

    private static void testAccessOrder() {
        LRU lru = new LRU(3);
        lru.put(1, 10);
        lru.put(2, 20);
        lru.put(3, 30);

        lru.get(1); // Access key 1, move to head
        lru.put(4, 40); // Should evict key 2

        assert lru.get(1) == 10 : "Recently accessed key 1 should exist";
        assert lru.get(2) == -1 : "Key 2 should be evicted";
        assert lru.get(3) == 30 : "Key 3 should exist";
        assert lru.get(4) == 40 : "Key 4 should exist";
        System.out.println("✓ testAccessOrder passed");
    }

    private static void testLargeCapacity() {
        LRU lru = new LRU(100);
        for (int i = 0; i < 100; i++) {
            lru.put(i, i * 10);
        }

        assert lru.size() == 100 : "Should hold exactly capacity items";

        lru.put(100, 1000);
        assert lru.get(0) == -1 : "First inserted key should be evicted";
        assert lru.get(99) == 990 : "Last inserted key should exist";
        assert lru.size() == 100 : "Size should stay at capacity";
        System.out.println("✓ testLargeCapacity passed");
    }

    private static void testInvalidCapacity() {
        try {
            new LRU(0);
            assert false : "Should throw IllegalArgumentException for capacity 0";
        } catch (IllegalArgumentException e) {
            assert e.getMessage().contains("Capacity must be > 0");
        }
        System.out.println("✓ testInvalidCapacity passed");
    }

    private static void testClear() {
        LRU lru = new LRU(2);
        lru.put(1, 10);
        lru.put(2, 20);

        lru.clear();
        assert lru.size() == 0 : "After clear, size should be 0";
        assert lru.get(1) == -1 : "After clear, keys should not exist";
        assert lru.containsKey(1) == false : "containsKey should return false";

        lru.put(3, 30); // Should work fine after clear
        assert lru.get(3) == 30 : "Should accept new inserts after clear";
        System.out.println("✓ testClear passed");
    }
}
