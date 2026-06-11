package dev.aahmedlab.lru;

import lombok.Getter;
import lombok.Setter;

/**
 * A doubly linked list node holding a key-value pair, used as the building block
 * of the recency list in {@link LRU} and {@link ThreadSafeLRU}.
 * <p>
 * <b>Design Decisions:</b>
 * <ul>
 *   <li><b>Immutable key:</b> The key never changes after construction, since it must stay
 *       consistent with the cache map entry pointing at this node.</li>
 *   <li><b>Mutable value and links:</b> Values are updated in place on {@code put}, and
 *       {@code prev}/{@code next} are relinked as the node moves within the recency list.</li>
 *   <li><b>Lombok accessors:</b> {@code @Getter}/{@code @Setter} keep the class free of
 *       boilerplate.</li>
 * </ul>
 * <p>
 * <b>Thread Safety:</b> This class is <em>not</em> thread-safe; callers must provide
 * external synchronization (as {@link ThreadSafeLRU} does).
 *
 * @param <T> the type of the key
 * @param <K> the type of the value
 */
public class Node<T, K> {
    @Getter
    private final T key;
    @Getter
    @Setter
    private K value;
    @Getter
    @Setter
    private Node<T, K> prev;
    @Getter
    @Setter
    private Node<T, K> next;

    public Node(T key, K value) {
        this.key = key;
        this.value = value;
    }
}
