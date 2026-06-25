package dev.aahmedlab.maxstack;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeMap;

/**
 * A thread-safe implementation of a max stack that supports O(log n) retrieval of the maximum
 * element.
 *
 * <p>This class maintains two synchronized data structures:
 *
 * <ul>
 *   <li>A {@link DoubleLinkedList} representing the stack order
 *   <li>A {@link TreeMap} mapping values to their occurrences for O(log n) max retrieval
 * </ul>
 *
 * <p><b>Concurrency Model:</b> Uses a single private lock object with intrinsic synchronization
 * ({@code synchronized}) on all public methods. This ensures a happens-before relationship between
 * reads and writes, preventing stale data visibility.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Single lock:</b> The invariant that elements appear in both structures simultaneously
 *       requires coordinated access, so per-structure locking would be insufficient.
 *   <li><b>Intrinsic synchronization:</b> Chosen over {@link
 *       java.util.concurrent.locks.ReentrantLock} for simplicity—no timeout, fairness, or condition
 *       variables are needed.
 *   <li><b>Private lock object:</b> Lock encapsulation prevents external code from acquiring the
 *       monitor, avoiding potential deadlocks or interference.
 *   <li><b>Unsynchronized DoubleLinkedList:</b> The underlying list is private and only accessed
 *       under the class's lock, so additional synchronization is unnecessary.
 * </ul>
 *
 * @param <T> the type of elements in this stack, must be {@link Comparable}
 */
class ConcurrentMaxStack<T extends Comparable<T>> {
    private final DoubleLinkedList<T> stack = new DoubleLinkedList<>();
    private final TreeMap<T, Deque<Node<T>>> maxStack = new TreeMap<>();
    private final Object lock = new Object();

    public void push(T value) {
        synchronized (lock) {
            Node<T> node = stack.push(value);
            maxStack.computeIfAbsent(value, _ -> new ArrayDeque<>()).push(node);
        }
    }

    public T pop() {
        synchronized (lock) {
            Node<T> node = stack.pop();
            if (node == null) return null;
            T value = node.getValue();
            Deque<Node<T>> items = maxStack.get(value);
            if (items != null) {
                if (!items.isEmpty()) items.pop();
                if (items.isEmpty()) maxStack.remove(value);
            }
            return value;
        }
    }

    public T top() {
        synchronized (lock) {
            return stack.peek();
        }
    }

    public T getMax() {
        synchronized (lock) {
            if (maxStack.isEmpty()) return null;
            return maxStack.lastKey();
        }
    }

    public T popMax() {
        synchronized (lock) {
            Map.Entry<T, Deque<Node<T>>> entry = maxStack.lastEntry();
            if (entry == null) return null;
            Deque<Node<T>> items = entry.getValue();
            if (items != null && !items.isEmpty()) {
                Node<T> nodeToRemove = items.pop();
                if (items.isEmpty()) maxStack.remove(entry.getKey());
                stack.removeNode(nodeToRemove);
            }
            return entry.getKey();
        }
    }
}
