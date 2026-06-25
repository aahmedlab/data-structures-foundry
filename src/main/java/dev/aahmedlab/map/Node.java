package dev.aahmedlab.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node<K, V> {
    private final K key;
    private final int hashKey;
    private V value;
    private Node<K, V> next;
}
