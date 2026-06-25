package dev.aahmedlab.maxstack;

import lombok.Getter;
import lombok.Setter;

/**
 * A doubly linked list node holding a single value, used as the building block of {@link
 * DoubleLinkedList} in the max stack implementations.
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Mutable links:</b> {@code prev}/{@code next} are relinked as nodes are pushed, popped,
 *       or removed from the middle of the list by {@code popMax}.
 *   <li><b>Exposed as a handle:</b> Instances are returned by {@link DoubleLinkedList#push} so that
 *       {@link MaxStackV2} and {@link ConcurrentMaxStack} can unlink a specific node in O(1)
 *       without searching.
 *   <li><b>Lombok accessors:</b> {@code @Getter}/{@code @Setter} keep the class free of
 *       boilerplate.
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is <em>not</em> thread-safe; callers must provide external
 * synchronization (as {@link ConcurrentMaxStack} does).
 *
 * @param <T> the type of the value held by this node
 */
public class Node<T> {
    @Getter
    @Setter
    private T value;
    @Getter
    @Setter
    private Node<T> prev;
    @Getter
    @Setter
    private Node<T> next;

    public Node(T value) {
        this.value = value;
    }
}
