package dev.aahmedlab.map;

import lombok.Getter;

@SuppressWarnings("unchecked")
public class ChainingHashMap<K, V> {
  private Node<K, V>[] slots;
  private int N = 64;
  @Getter private int size;
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
    Node<K, V>[] oldSlots = slots;
    slots = (Node<K, V>[]) new Node[newCapacity];
    N = newCapacity;
    threshold = (int) (N * loadFactor);
    for (Node<K, V> node : oldSlots) {
      while (node != null) {
        Node<K, V> next = node.getNext();
        int index = indexFor(node.getHashKey());
        node.setNext(slots[index]);
        slots[index] = node;
        node = next;
      }
    }
  }

  private int indexFor(int hash) {
      /*
       The power-of-two design: & (N-1) does
       the sign-stripping and
       the range-bounding and
       the index calculation in one instruction
       */
      return hash & (N - 1); // N must be a power of two.
  }

  private int spread(K key) {
    int h = key.hashCode(); // ignoring null-key for now
    return h ^ (h >>> 16); // spread the key
  }
}
