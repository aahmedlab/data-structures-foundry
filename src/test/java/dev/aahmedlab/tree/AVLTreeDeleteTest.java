package dev.aahmedlab.tree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AVLTreeDeleteTest {

  private static AVLTree<Integer, String> treeOf(int... keys) {
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int k : keys) {
      tree.insert(k, "v" + k);
    }
    return tree;
  }

  /** Returns key -> value pairs in in-order traversal order. */
  private static <K extends Comparable<K>, V> Map<K, V> inOrder(AVLTree<K, V> tree) {
    Map<K, V> out = new LinkedHashMap<>();
    collect(tree.root(), out);
    return out;
  }

  private static <K extends Comparable<K>, V> void collect(AVLNode<K, V> n, Map<K, V> out) {
    if (n == null) return;
    collect(n.left, out);
    out.put(n.key, n.value);
    collect(n.right, out);
  }

  /** Verifies BST ordering, stored heights, and AVL balance; returns the subtree height. */
  private static <K extends Comparable<K>, V> int assertAvl(AVLNode<K, V> n, K lo, K hi) {
    if (n == null) return -1;
    if (lo != null) assertTrue(n.key.compareTo(lo) > 0, "BST order violated at " + n.key);
    if (hi != null) assertTrue(n.key.compareTo(hi) < 0, "BST order violated at " + n.key);

    int lh = assertAvl(n.left, lo, n.key);
    int rh = assertAvl(n.right, n.key, hi);

    assertEquals(1 + Math.max(lh, rh), n.height, "Stale height at " + n.key);
    assertTrue(Math.abs(lh - rh) <= 1, "Unbalanced at " + n.key + " (bf=" + (lh - rh) + ")");
    return n.height;
  }

  private static <K extends Comparable<K>, V> void assertAvl(AVLTree<K, V> tree) {
    assertAvl(tree.root(), null, null);
  }

  @Test
  void testDeleteFromEmptyTreeIsNoOp() {
    AVLTree<Integer, String> tree = new AVLTree<>();
    tree.delete(42);

    assertNull(tree.root());
  }

  @Test
  void testDeleteMissingKeyLeavesTreeUnchanged() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5, 15);
    Map<Integer, String> before = inOrder(tree);

    tree.delete(99);
    tree.delete(12);

    assertEquals(before, inOrder(tree));
    assertAvl(tree);
  }

  @Test
  void testDeleteOnlyNodeEmptiesTree() {
    AVLTree<Integer, String> tree = treeOf(1);
    tree.delete(1);

    assertNull(tree.root());
  }

  @Test
  void testDeleteLeaf() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30);
    tree.delete(10);

    assertEquals(20, tree.root().key);
    assertNull(tree.root().left);
    assertEquals(30, tree.root().right.key);
    assertAvl(tree);
  }

  @Test
  void testDeleteNodeWithOnlyLeftChild() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5);
    tree.delete(10);

    assertEquals(5, tree.root().left.key);
    assertEquals("v5", tree.root().left.value);
    assertAvl(tree);
  }

  @Test
  void testDeleteNodeWithOnlyRightChild() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 35);
    tree.delete(30);

    assertEquals(35, tree.root().right.key);
    assertEquals("v35", tree.root().right.value);
    assertAvl(tree);
  }

  @Test
  void testDeleteNodeWithTwoChildrenUsesInOrderSuccessor() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 25, 35);
    tree.delete(20);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(25, root.key, "Root should be replaced by its in-order successor");
    assertEquals("v25", root.value, "Successor's value must move along with its key");
    assertEquals(List.of(10, 25, 30, 35), new ArrayList<>(inOrder(tree).keySet()));
    assertAvl(tree);
  }

  @Test
  void testDeletePreservesValuesOfRemainingKeys() {
    AVLTree<Integer, String> tree = treeOf(50, 30, 70, 20, 40, 60, 80);
    tree.delete(30);
    tree.delete(70);

    Map<Integer, String> expected = new LinkedHashMap<>();
    for (int k : new int[] {20, 40, 50, 60, 80}) expected.put(k, "v" + k);
    assertEquals(expected, inOrder(tree));
  }

  @Test
  void testDeleteTriggersRightRotation() {
    // 20 has left-heavy child 10 (bf +1) after removing 30 -> LL case.
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5);
    tree.delete(30);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(10, root.key);
    assertEquals(5, root.left.key);
    assertEquals(20, root.right.key);
    assertAvl(tree);
  }

  @Test
  void testDeleteTriggersLeftRotation() {
    // RR case: removing 5 leaves 10 right-heavy with right child 20 (bf -1).
    AVLTree<Integer, String> tree = treeOf(10, 5, 20, 30);
    tree.delete(5);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(20, root.key);
    assertEquals(10, root.left.key);
    assertEquals(30, root.right.key);
    assertAvl(tree);
  }

  @Test
  void testDeleteTriggersLeftRightRotation() {
    // LR case: left child 10 is right-heavy (has only 15).
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 15);
    tree.delete(30);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(15, root.key);
    assertEquals(10, root.left.key);
    assertEquals(20, root.right.key);
    assertAvl(tree);
  }

  @Test
  void testDeleteTriggersRightLeftRotation() {
    // RL case: right child 20 is left-heavy (has only 15).
    AVLTree<Integer, String> tree = treeOf(10, 5, 20, 15);
    tree.delete(5);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(15, root.key);
    assertEquals(10, root.left.key);
    assertEquals(20, root.right.key);
    assertAvl(tree);
  }

  @Test
  void testDeleteWithBalancedChildUsesSingleRotation() {
    // Only reachable via delete: the heavy side's child has bf 0. A single right rotation
    // is correct here; a double rotation would leave the tree unbalanced.
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5, 15);
    tree.delete(30);

    AVLNode<Integer, String> root = tree.root();
    assertEquals(10, root.key);
    assertEquals(5, root.left.key);
    assertEquals(20, root.right.key);
    assertEquals(15, root.right.left.key);
    assertEquals(2, root.height);
    assertAvl(tree);
  }

  @Test
  void testDeleteRebalancesAtMultipleLevels() {
    // Minimal (Fibonacci-shaped) AVL tree of height 4. Deleting 12 unbalances 11 (rotation
    // shrinks that subtree), which then unbalances the root and forces a second rotation.
    AVLTree<Integer, String> tree = treeOf(8, 5, 11, 3, 7, 10, 12, 2, 4, 6, 9, 1);
    assertEquals(4, tree.root().height);

    tree.delete(12);

    assertEquals(5, tree.root().key, "Root should have rotated after the subtree rotation");
    assertEquals(3, tree.root().height);
    assertAvl(tree);
    assertEquals(
        List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), new ArrayList<>(inOrder(tree).keySet()));
  }

  @Test
  void testDeleteSameKeyTwice() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.delete(2);
    tree.delete(2);

    assertEquals(List.of(1, 3), new ArrayList<>(inOrder(tree).keySet()));
    assertAvl(tree);
  }

  @Test
  void testDeleteAllKeysInAscendingOrderEmptiesTree() {
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int i = 0; i < 100; i++) tree.insert(i, "v" + i);

    for (int i = 0; i < 100; i++) {
      tree.delete(i);
      assertAvl(tree);
    }
    assertNull(tree.root());
  }

  @Test
  void testDeleteAllKeysInDescendingOrderEmptiesTree() {
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int i = 0; i < 100; i++) tree.insert(i, "v" + i);

    for (int i = 99; i >= 0; i--) {
      tree.delete(i);
      assertAvl(tree);
    }
    assertNull(tree.root());
  }

  @Test
  void testReinsertAfterDelete() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.delete(2);
    tree.insert(2, "new");

    assertEquals("new", inOrder(tree).get(2));
    assertAvl(tree);
  }

  @ParameterizedTest
  @ValueSource(longs = {1L, 7L, 42L, 1234L, 99999L})
  void testRandomInsertDeleteMatchesTreeMap(long seed) {
    Random rnd = new Random(seed);
    AVLTree<Integer, String> tree = new AVLTree<>();
    TreeMap<Integer, String> reference = new TreeMap<>();

    for (int i = 0; i < 2000; i++) {
      int key = rnd.nextInt(300);
      if (rnd.nextInt(3) == 0) {
        tree.delete(key);
        reference.remove(key);
      } else {
        String value = "v" + i;
        tree.insert(key, value);
        reference.put(key, value);
      }
      assertAvl(tree);
    }
    assertEquals(reference, new TreeMap<>(inOrder(tree)));

    List<Integer> keys = new ArrayList<>(reference.keySet());
    Collections.shuffle(keys, rnd);
    for (int key : keys) {
      tree.delete(key);
      reference.remove(key);
      assertAvl(tree);
      assertEquals(reference, new TreeMap<>(inOrder(tree)));
    }
    assertNull(tree.root());
  }
}
