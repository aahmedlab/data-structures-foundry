package dev.aahmedlab.autocomplete;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class TrieNodeTest {

  @Test
  void testSortedReturnsWordsByFrequencyDescending() {
    TrieNode node = new TrieNode(3);
    node.updateCache("a", 1);
    node.updateCache("b", 3);
    node.updateCache("c", 2);

    assertEquals(List.of("b", "c", "a"), node.sorted());
  }

  @Test
  void testKeepsOnlyTopKByFrequency() {
    TrieNode node = new TrieNode(2);
    node.updateCache("low", 1);
    node.updateCache("mid", 2);
    node.updateCache("high", 3); // should evict "low" (lowest freq)

    assertEquals(List.of("high", "mid"), node.sorted());
  }

  @Test
  void testLowerFrequencyDoesNotDisplaceWhenFull() {
    TrieNode node = new TrieNode(2);
    node.updateCache("a", 5);
    node.updateCache("b", 4);
    node.updateCache("c", 1); // below the current minimum, must be ignored

    assertEquals(List.of("a", "b"), node.sorted());
  }

  @Test
  void testUpdatingExistingWordRefreshesFrequency() {
    TrieNode node = new TrieNode(3);
    node.updateCache("word", 1);
    node.updateCache("word", 10); // same word, new frequency

    assertEquals(List.of("word"), node.sorted(), "Word should not be duplicated");
  }

  @Test
  void testTiesBrokenByWordAscending() {
    TrieNode node = new TrieNode(3);
    node.updateCache("banana", 5);
    node.updateCache("apple", 5);

    assertEquals(List.of("apple", "banana"), node.sorted(), "Equal freq breaks ties by word asc");
  }

  @Test
  void testEmptyNodeSortsToEmptyList() {
    assertTrue(new TrieNode(3).sorted().isEmpty());
  }

  @Test
  void testInvalidKThrows() {
    assertThrows(IllegalArgumentException.class, () -> new TrieNode(0));
    assertThrows(IllegalArgumentException.class, () -> new TrieNode(-1));
  }
}
