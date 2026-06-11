package dev.aahmedlab.lru;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A thread-safe least-recently-used (LRU) cache with O(1) get and put operations
 * and a fixed maximum capacity.
 * <p>
 * This class maintains two coordinated data structures:
 * <ul>
 *   <li>A {@link HashMap} mapping keys to their nodes for O(1) lookup</li>
 *   <li>A doubly linked list of {@link Node}s ordered by recency, bounded by
 *       sentinel head (most recent) and tail (least recent) nodes</li>
 * </ul>
 * <p>
 * <b>Concurrency Model:</b>
 * A single {@link ReentrantLock} guards every public operation. Both the map and the
 * recency list must change together (even {@code get} reorders the list), so all
 * accesses—reads included—are mutually exclusive.
 * <p>
 * <b>Design Decisions:</b>
 * <ul>
 *   <li><b>Single lock:</b> The map and the linked list form one invariant; per-structure
 *       or read-write locking would allow torn states because {@code get} also mutates.</li>
 *   <li><b>{@link ReentrantLock} over {@code synchronized}:</b> Keeps the lock explicit
 *       and leaves room for timed or interruptible acquisition if needed later.</li>
 *   <li><b>Sentinel nodes:</b> Permanent head/tail sentinels eliminate null checks
 *       and edge cases when linking and unlinking at the list boundaries.</li>
 *   <li><b>Eviction from tail:</b> When capacity is exceeded, the node before the tail
 *       sentinel (the least recently used entry) is unlinked and removed from the map.</li>
 *   <li><b>{@code -1} as a miss sentinel:</b> {@link #get(int)} returns {@code -1} for
 *       absent keys, so values of {@code -1} cannot be distinguished from misses.</li>
 * </ul>
 */
public class ConcurrentLRU {
    private final Map<Integer, Node<Integer, Integer>> cache;
    private final int capacity;
    private final Node<Integer, Integer> head;
    private final Node<Integer, Integer> tail;
    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentLRU(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
        this.capacity = capacity;
        this.cache = new HashMap<>();
        this.head = new Node<>(-1, -1); // Sentinel: most recently used boundary
        this.tail = new Node<>(-1, -1); // Sentinel: least recently used boundary
        this.head.setNext(tail);
        this.tail.setPrev(head);
    }

    public int get(int key) {
        lock.lock();
        try {
            Node<Integer, Integer> node = cache.get(key);
            if (node == null) return -1;

            moveToHead(node); // Mark as recently used
            return node.getValue();
        } finally {
            lock.unlock();
        }
    }

    public void put(int key, int value) {
        lock.lock();
        try {
            Node<Integer, Integer> existing = cache.get(key);
            if (existing != null) {
                // Update: change value and mark as recently used
                existing.setValue(value);
                moveToHead(existing);
            } else {
                // Insert: add new node, evict if over capacity
                Node<Integer, Integer> newNode = new Node<>(key, value);
                cache.put(key, newNode);
                addToHead(newNode);

                if (cache.size() > capacity) {
                    Node<Integer, Integer> lru = tail.getPrev(); // Least recently used (rightmost)
                    removeNode(lru);
                    cache.remove(lru.getKey());
                }
            }
        } finally {
            lock.unlock();
        }
    }

    // Move existing node to head (mark as recently used)
    private void moveToHead(Node<Integer, Integer> node) {
        removeNode(node); // Unlink from current position
        addToHead(node);  // Relink at head
    }

    // Insert node right after head (most recently used position)
    private void addToHead(Node<Integer, Integer> node) {
        node.setPrev(head);
        node.setNext(head.getNext());
        head.getNext().setPrev(node);
        head.setNext(node);
    }

    // Unlink node from list (assumes node is in the list)
    private void removeNode(Node<Integer, Integer> node) {
        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());
    }

    public int size() {
        lock.lock();
        try {
            return cache.size();
        } finally {
            lock.unlock();
        }
    }

    public boolean containsKey(int key) {
        lock.lock();
        try {
            return cache.containsKey(key);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            cache.clear();
            head.setNext(tail);
            tail.setPrev(head);
        } finally {
            lock.unlock();
        }

    }
}
