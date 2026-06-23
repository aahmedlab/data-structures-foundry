package dev.aahmedlab.frequentelement;

import java.util.*;

/**
 * Solutions for finding the {@code k} most frequent elements in an integer array, offering two
 * algorithmic strategies with different complexity trade-offs.
 *
 * <p>Both approaches first build a {@link HashMap} of element frequencies, then differ in how they
 * select the top {@code k}:
 *
 * <ul>
 *   <li>{@link #topKFrequentUsingBucket(int[], int)} — bucket sort by freq, O(n)
 *   <li>{@link #topKFrequentUsingHeap(int[], int)} — bounded min-heap, O(n log k)
 * </ul>
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Bucket sort:</b> A freq can never exceed {@code nums.length}, so an array of {@code
 *       nums.length + 1} buckets indexed by freq yields linear-time selection by scanning from
 *       the highest bucket down.
 *   <li><b>Min-heap of size {@code k}:</b> Keeping only the {@code k} largest entries (evicting the
 *       smallest on overflow) bounds memory to O(k) beyond the freq map, which is preferable
 *       when {@code k} is much smaller than the number of distinct elements.
 * </ul>
 *
 * <p><b>Thread Safety:</b> Both methods are stateless and side-effect free, so instances may be
 * shared freely across threads.
 */
public class TopKFrequentElements {
  public int[] topKFrequentUsingBucket(int[] nums, int k) {
    Map<Integer, Integer> frequency = new HashMap<>();
    for (int num : nums) {
      frequency.put(num, frequency.getOrDefault(num, 0) + 1);
    }

    List<Integer>[] buckets = new List[nums.length + 1];
    for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
      if (buckets[entry.getValue()] == null) {
        buckets[entry.getValue()] = new ArrayList<>();
      }
      buckets[entry.getValue()].add(entry.getKey());
    }

    int[] topKFrequentElements = new int[k];
    int j = 0;
    for (int i = buckets.length - 1; i >= 0; i--) {
      if (j >= k) break;
      if (buckets[i] != null) {
        for (int key : buckets[i]) {
          if (j >= k) break;
          topKFrequentElements[j++] = key;
        }
      }
    }
    return topKFrequentElements;
  }

  public int[] topKFrequentUsingHeap(int[] nums, int k) {
    Map<Integer, Integer> frequency = new HashMap<>();
    PriorityQueue<Map.Entry<Integer, Integer>> minHeap =
        new PriorityQueue<>(Comparator.comparingInt(Map.Entry::getValue));
    int[] topKFrequentElements = new int[k];
    for (int num : nums) {
      frequency.put(num, frequency.getOrDefault(num, 0) + 1);
    }

    for (Map.Entry<Integer, Integer> entry : frequency.entrySet()) {
      minHeap.offer(entry);
      if (minHeap.size() > k) {
        minHeap.poll();
      }
    }

    int i = 0;
    while (!minHeap.isEmpty()) {
      Map.Entry<Integer, Integer> entry = minHeap.poll();
      topKFrequentElements[i++] = entry.getKey();
    }
    return topKFrequentElements;
  }
}
