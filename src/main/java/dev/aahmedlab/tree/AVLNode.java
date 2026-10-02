package dev.aahmedlab.tree;

public class AVLNode<K extends Comparable<K>, V> {
  K key;
  V value;
  AVLNode<K, V> left, right;
  int height;

  public AVLNode(K key, V value) {
    this.key = key;
    this.value = value;
    this.height = 0;
    this.left = null;
    this.right = null;
  }
}
