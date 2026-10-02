package dev.aahmedlab.tree;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AVLTreeLookupTest {

  private static AVLTree<Integer, String> treeOf(int... keys) {
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int k : keys) {
      tree.insert(k, "v" + k);
    }
    return tree;
  }

  // ---- get ----

  @Test
  void testGetOnEmptyTreeReturnsNull() {
    AVLTree<Integer, String> tree = new AVLTree<>();

    assertNull(tree.get(1));
  }

  @Test
  void testGetReturnsValueForEveryInsertedKey() {
    AVLTree<Integer, String> tree = treeOf(50, 30, 70, 20, 40, 60, 80);

    for (int k : new int[] {50, 30, 70, 20, 40, 60, 80}) {
      assertEquals("v" + k, tree.get(k));
    }
  }

  @Test
  void testGetMissingKeyReturnsNull() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30);

    assertNull(tree.get(5), "Smaller than every key");
    assertNull(tree.get(15), "Between existing keys");
    assertNull(tree.get(35), "Larger than every key");
  }

  @Test
  void testGetReturnsUpdatedValueAfterReinsert() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.insert(2, "updated");

    assertEquals("updated", tree.get(2));
  }

  @Test
  void testGetAfterDeleteReturnsNull() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30);
    tree.delete(10);

    assertNull(tree.get(10));
    assertEquals("v20", tree.get(20));
    assertEquals("v30", tree.get(30));
  }

  @Test
  void testGetFindsKeysMovedByRotations() {
    // Ascending inserts force repeated left rotations.
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int i = 0; i < 64; i++) tree.insert(i, "v" + i);

    for (int i = 0; i < 64; i++) {
      assertEquals("v" + i, tree.get(i));
    }
  }

  @Test
  void testGetFindsSuccessorAfterTwoChildDelete() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 25, 35);
    tree.delete(20);

    assertNull(tree.get(20));
    assertEquals("v25", tree.get(25), "Successor's value should move with its key");
  }

  // ---- containsKey ----

  @Test
  void testContainsKeyOnEmptyTree() {
    AVLTree<Integer, String> tree = new AVLTree<>();

    assertFalse(tree.containsKey(1));
  }

  @Test
  void testContainsKeyForPresentAndMissingKeys() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30);

    assertTrue(tree.containsKey(10));
    assertTrue(tree.containsKey(20));
    assertTrue(tree.containsKey(30));
    assertFalse(tree.containsKey(15));
  }

  @Test
  void testContainsKeyIsTrueForKeyMappedToNull() {
    AVLTree<Integer, String> tree = new AVLTree<>();
    tree.insert(1, null);

    assertNull(tree.get(1));
    assertTrue(tree.containsKey(1), "A key mapped to null is still present");
  }

  @Test
  void testContainsKeyIsFalseAfterDelete() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.delete(2);

    assertFalse(tree.containsKey(2));
  }

  // ---- size ----

  @Test
  void testSizeOfEmptyTreeIsZero() {
    assertEquals(0, new AVLTree<Integer, String>().size());
  }

  @Test
  void testSizeCountsDistinctInserts() {
    AVLTree<Integer, String> tree = treeOf(5, 3, 8, 1);

    assertEquals(4, tree.size());
  }

  @Test
  void testSizeUnchangedWhenReinsertingExistingKey() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.insert(2, "updated");

    assertEquals(3, tree.size());
  }

  @Test
  void testSizeDecrementsForLeafOneChildAndTwoChildDeletes() {
    // Shape: 20 -> (10 -> 5), (30 -> 25, 35)
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5, 25, 35);
    assertEquals(6, tree.size());

    tree.delete(5); // leaf
    assertEquals(5, tree.size());

    tree.delete(10); // now a leaf; root rotates left, giving 30 -> (20 -> _, 25), 35
    assertEquals(4, tree.size());

    tree.delete(30); // root with two children: must count once, not twice
    assertEquals(3, tree.size());

    tree.delete(20); // one child (25)
    assertEquals(2, tree.size());
  }

  @Test
  void testSizeDecrementsOnceForNodeWithOneChild() {
    AVLTree<Integer, String> tree = treeOf(20, 10, 30, 5);
    tree.delete(10); // 10 has only a left child

    assertEquals(3, tree.size());
  }

  @Test
  void testSizeUnchangedWhenDeletingMissingKey() {
    AVLTree<Integer, String> tree = treeOf(1, 2, 3);
    tree.delete(99);
    tree.delete(2);
    tree.delete(2);

    assertEquals(2, tree.size());
  }

  @Test
  void testSizeReturnsToZeroAfterDeletingEverything() {
    AVLTree<Integer, String> tree = new AVLTree<>();
    for (int i = 0; i < 50; i++) tree.insert(i, "v" + i);
    for (int i = 0; i < 50; i++) tree.delete(i);

    assertEquals(0, tree.size());
    assertNull(tree.root());
  }

  // ---- all three against a reference map ----

  @ParameterizedTest
  @ValueSource(longs = {3L, 17L, 2024L})
  void testRandomOperationsMatchTreeMap(long seed) {
    Random rnd = new Random(seed);
    AVLTree<Integer, String> tree = new AVLTree<>();
    TreeMap<Integer, String> reference = new TreeMap<>();

    for (int i = 0; i < 3000; i++) {
      int key = rnd.nextInt(200);
      if (rnd.nextInt(3) == 0) {
        tree.delete(key);
        reference.remove(key);
      } else {
        String value = "v" + i;
        tree.insert(key, value);
        reference.put(key, value);
      }

      assertEquals(reference.size(), tree.size());
      int probe = rnd.nextInt(200);
      assertEquals(reference.get(probe), tree.get(probe));
      assertEquals(reference.containsKey(probe), tree.containsKey(probe));
    }
  }
}
