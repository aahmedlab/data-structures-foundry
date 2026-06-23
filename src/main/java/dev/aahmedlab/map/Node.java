package dev.aahmedlab.map;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Node<K, V> {
  private final K key;
  private V value;
  private final int hashKey;
  private Node<K, V> next;
}
