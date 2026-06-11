package dev.aahmedlab.maxstack;

import java.util.ArrayDeque;

/**
 * A sentinel-bounded doubly linked list used as the stack backbone for
 * {@link MaxStackV2} and {@link ConcurrentMaxStack}.
 * <p>
 * The head sentinel marks the top of the stack; new elements are inserted right
 * after it and popped from the same position (LIFO order).
 * <p>
 * <b>Design Decisions:</b>
 * <ul>
 *   <li><b>Sentinel nodes:</b> Permanent head/tail sentinels eliminate null checks
 *       and edge cases when linking and unlinking at the list boundaries.</li>
 *   <li><b>Node handles:</b> {@link #push(Object)} returns the created {@link Node} so
 *       callers can later unlink it in O(1) via {@link #removeNode(Node)} without
 *       searching the list—this is what makes {@code popMax} efficient.</li>
 *   <li><b>Public {@link #removeNode(Node)}:</b> Trusts callers to pass nodes that are
 *       currently linked; no membership validation is performed.</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is <em>not</em> thread-safe; callers must provide
 * external synchronization (as {@link ConcurrentMaxStack} does).
 *
 * @param <T> the type of elements held in the list
 */
public class DoubleLinkedList<T> {
    private Node<T> head;
    private Node<T> tail;

    public DoubleLinkedList() {
        this.head = new Node<>(null); // Sentinel: most recently used boundary
        this.tail = new Node<>(null); // Sentinel: least recently used boundary
        this.head.setNext(tail);
        this.tail.setPrev(head);
    }

    public Node<T> push(T value){
        Node<T> node = new Node<>(value);
        addToHead(node);
        return node;
    }

    public Node<T> pop() {
        if (head.getNext() == null || head.getNext() == tail) return null;
        return removeNode(head.getNext());
    }

    public T peek() {
        if (head.getNext() == null || head.getNext() == tail) return null;
        return head.getNext().getValue();
    }

    private void addToHead(Node<T> node) {
        node.setPrev(head);
        node.setNext(head.getNext());
        head.getNext().setPrev(node);
        head.setNext(node);
    }

    public Node<T> removeNode(Node<T> node) {
        node.getPrev().setNext(node.getNext());
        node.getNext().setPrev(node.getPrev());
        return node;
    }
}
