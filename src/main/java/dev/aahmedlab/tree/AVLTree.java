package dev.aahmedlab.tree;

public class AVLTree<K extends Comparable<K>, V> {
  private AVLNode<K, V> root;
  private int size;

  public void insert(K key, V value) {
    root = insert(root, key, value);
  }

  public void delete(K key) {
    root = delete(root, key);
  }

  public V get(K key) {
    AVLNode<K, V> node = findNode(key);
    return node == null ? null : node.value;
  }

  public boolean containsKey(K key) {
    return findNode(key) != null;
  }

  public int size() {
    return size;
  }

  private AVLNode<K, V> findNode(K key) {
    AVLNode<K, V> curr = root;
    while (curr != null) {
      int cmp = key.compareTo(curr.key);
      if (cmp == 0) return curr;
      curr = cmp < 0 ? curr.left : curr.right;
    }
    return null;
  }

  AVLNode<K, V> root() {
    return root;
  }

  private AVLNode<K, V> insert(AVLNode<K, V> node, K key, V value) {
    if (node == null) {
      size++;
      return new AVLNode<>(key, value);
    }

    if (key.compareTo(node.key) < 0) {
      node.left = insert(node.left, key, value);
    } else if (key.compareTo(node.key) > 0) {
      node.right = insert(node.right, key, value);
    } else {
      node.value = value;
      return node;
    }
    return rebalance(node);
  }

  private AVLNode<K, V> delete(AVLNode<K, V> node, K key) {
    if (node == null) return null;

    if (key.compareTo(node.key) < 0) {
      node.left = delete(node.left, key);
    } else if (key.compareTo(node.key) > 0) {
      node.right = delete(node.right, key);
    } else {
      // The two-child case removes no node here; the recursive successor delete decrements size.
      if (node.left == null || node.right == null) {
        size--;
        return node.left == null ? node.right : node.left;
      } else {
        AVLNode<K, V> successorNode = findMin(node.right);
        node.key = successorNode.key;
        node.value = successorNode.value;
        node.right = delete(node.right, successorNode.key);
      }
    }
    return rebalance(node);
  }

  private AVLNode<K, V> findMin(AVLNode<K, V> node) {
    AVLNode<K, V> curr = node;
    while (curr.left != null) {
      curr = curr.left;
    }
    return curr;
  }

  private AVLNode<K, V> rebalance(AVLNode<K, V> node) {
    node.height = 1 + Math.max(height(node.left), height(node.right));

    int bf = balanceFactor(node);
    if (bf > 1) {
      int childBF = balanceFactor(node.left);
      if (childBF < 0) node.left = rotateLeft(node.left);
      return rotateRight(node);
    } else if (bf < -1) {
      int childBF = balanceFactor(node.right);
      if (childBF > 0) node.right = rotateRight(node.right);
      return rotateLeft(node);
    }
    return node;
  }

  private int height(AVLNode<K, V> n) {
    return n == null ? -1 : n.height;
  }

  private int balanceFactor(AVLNode<K, V> n) {
    return n == null ? 0 : height(n.left) - height(n.right);
  }

  private AVLNode<K, V> rotateLeft(AVLNode<K, V> xNode) {
    AVLNode<K, V> pivotNode = xNode.right;
    xNode.right = pivotNode.left;
    pivotNode.left = xNode;

    xNode.height = 1 + Math.max(height(xNode.left), height(xNode.right));
    pivotNode.height = 1 + Math.max(height(pivotNode.left), height(pivotNode.right));

    return pivotNode;
  }

  private AVLNode<K, V> rotateRight(AVLNode<K, V> xNode) {
    AVLNode<K, V> pivotNode = xNode.left;
    xNode.left = pivotNode.right;
    pivotNode.right = xNode;

    xNode.height = 1 + Math.max(height(xNode.left), height(xNode.right));
    pivotNode.height = 1 + Math.max(height(pivotNode.left), height(pivotNode.right));

    return pivotNode;
  }
}
