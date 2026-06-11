package dev.aahmedlab.maxstack;

import java.util.*;

/**
 * A single-threaded max stack that supports {@code popMax} in O(log n) by removing
 * the maximum element from anywhere in the stack.
 * <p>
 * This class maintains two coordinated data structures:
 * <ul>
 *   <li>A {@link DoubleLinkedList} representing the stack order</li>
 *   <li>A {@link TreeMap} mapping values to a {@link Deque} of their nodes, with the most
 *       recent occurrence on top, for O(log n) max retrieval and removal</li>
 * </ul>
 * <p>
 * <b>Design Decisions:</b>
 * <ul>
 *   <li><b>Linked list over array stack:</b> {@code popMax} must unlink an element from the
 *       middle of the stack; a doubly linked list does this in O(1) given the node.</li>
 *   <li><b>TreeMap of node deques:</b> {@code lastKey()}/{@code lastEntry()} locate the max
 *       in O(log n), and the per-value deque resolves duplicates to the most recently
 *       pushed occurrence.</li>
 *   <li><b>Node handles:</b> {@link DoubleLinkedList#push} returns the created {@link Node},
 *       letting the TreeMap reference list positions directly without searching.</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is <em>not</em> thread-safe.
 * See {@link ConcurrentMaxStack} for a concurrent variant (same implementation, synchronized).
 *
 * @param <T> the type of elements in this stack, must be {@link Comparable}
 */
class PopMaxStack<T extends Comparable<T>> {
    private final DoubleLinkedList<T> stack = new DoubleLinkedList<>();
    private final TreeMap<T, Deque<Node<T>>> maxStack = new TreeMap<>();

    public void push(T value){
        Node<T> node = stack.push(value);
        maxStack.computeIfAbsent(value, _ -> new ArrayDeque<>()).push(node);
    }

    public T pop() {
        Node<T> node = stack.pop();
        if (node != null && maxStack.containsKey(node.getValue())) {
            Deque<Node<T>> items = maxStack.get(node.getValue());
            if (!items.isEmpty()) items.pop();
            if (items.isEmpty()) maxStack.remove(node.getValue());
        }
        return node == null? null:node.getValue();
    }

    public T top() {
        return stack.peek();
    }

    public T getMax() {
        if (maxStack.isEmpty()) return null;
        return maxStack.lastKey();
    }

    public T popMax() {
        if (maxStack.isEmpty()) return null;
        Map.Entry<T, Deque<Node<T>>> entry = maxStack.lastEntry();
        Deque<Node<T>> items = entry.getValue();
        if (items != null && !items.isEmpty()) {
            Node<T> nodeToRemove = items.pop();
            if (items.isEmpty()) maxStack.remove(entry.getKey());
            stack.removeNode(nodeToRemove);
        }
        return entry.getKey();
    }
}
