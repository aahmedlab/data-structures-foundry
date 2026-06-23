package dev.aahmedlab.map;

import java.util.Arrays;

@SuppressWarnings("unchecked")
public class ChainingHashMap<K, V> {
  private Node<K, V>[] slots;
  private int N = 64;
  private int size;
  private int threshold;
  private final double loadFactor = 0.75;

  public ChainingHashMap() {
    slots = new Node[N];
    threshold = (int) (N * loadFactor);
  }

  public void put(K key, V value) {
    if (key == null) throw new NullPointerException("key is null");
    int hashedKey = spread(key);
    int index = indexFor(hashedKey);

    Node<K, V> node = slots[index];
    while (node != null) {
      if (node.getHashKey() == hashedKey && node.getKey().equals(key)) {
        node.setValue(value);
        return;
      }
      node = node.getNext();
    }

    slots[index] = new Node<>(key, value, hashedKey, slots[index]);
    size++;
    if (size > threshold) {
      resize(N * 2);
    }
  }

  public V get(K key) {
    if (key == null) throw new NullPointerException("key is null");
    int hashedKey = spread(key);
    int index = indexFor(hashedKey);

    Node<K, V> node = slots[index];
    while (node != null) {
      if (node.getHashKey() == hashedKey && node.getKey().equals(key)) {
        return node.getValue();
      }
      node = node.getNext();
    }
    return null;
  }

  public V remove(K key) {
    if (key == null) throw new NullPointerException("key is null");
    int hashedKey = spread(key);
    int index = indexFor(hashedKey);
    Node<K, V> prev = null;
    Node<K, V> curr = slots[index];
    while (curr != null) {
      if (curr.getHashKey() == hashedKey && curr.getKey().equals(key)) {
        V oldValue = curr.getValue();
        if (prev != null) {
          prev.setNext(curr.getNext());
        } else {
          slots[index] = curr.getNext();
        }
        size--;
        return oldValue;
      }
      prev = curr;
      curr = curr.getNext();
    }
    return null;
  }

  private void resize(int newCapacity) {
    Node<K, V>[] copiedSlots = Arrays.copyOf(slots, slots.length);
    slots = new Node[newCapacity];
    N = newCapacity;
    threshold = (int) (N * loadFactor);
    for (Node<K, V> node : copiedSlots) {
      if (node != null) {
        int index = indexFor(node.getHashKey());
        slots[index] = node;
      }
    }
  }

  private int getIndex(Node<K, V> node) {
    return indexFor(node.getHashKey());
  }

  private int getIndex(K key) {
    return indexFor(spread(key));
  }

  private int indexFor(int hash) {
    return (hash & 0x7fffffff) % N; // strip sign + mod
  }

  private int spread(K key) {
    int h = key.hashCode(); // ignoring null-key for now
    return h ^ (h >>> 16); // spread the key
  }
}
