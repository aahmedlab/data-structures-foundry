package dev.aahmedlab.frequentelement;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TopKFrequentElementsTest {
    private TopKFrequentElements topKFrequentElements;

    @BeforeEach
    void setUp() {
        topKFrequentElements = new TopKFrequentElements();
    }

    @Test
    void testInitialState() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{}, K);
        assertArrayEquals(new int[K], result);
    }

    @Test
    void testSingleHit() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 1, 1, 2, 2, 3}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void testSingleElement() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{5}, K);
        assertArrayEquals(new int[]{5}, result);
    }

    @Test
    void testAllSameElement() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{7, 7, 7, 7, 7}, K);
        assertArrayEquals(new int[]{7}, result);
    }

    @Test
    void testKEqualsOne() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 2, 3, 3, 3, 4, 4}, K);
        assertArrayEquals(new int[]{3}, result);
    }

    @Test
    void testKEqualsUniqueElements() {
        int K = 3;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 2, 3}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    @Test
    void testKGreaterThanUniqueElements() {
        int K = 5;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 1, 2}, K);
        assertEquals(K, result.length);
    }

    @Test
    void testMultipleElementsWithDifferentFrequencies() {
        int K = 3;
        int[] result =
                topKFrequentElements.topKFrequentUsingHeap(new int[]{4, 4, 4, 4, 3, 3, 3, 2, 2, 1}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{2, 3, 4}, result);
    }

    @Test
    void testAllElementsSameFrequency() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 2, 3, 4}, K);
        assertEquals(K, result.length);
    }

    @Test
    void testLargeArray() {
        int K = 2;
        int[] nums = new int[100];
        Arrays.fill(nums, 0, 50, 1);
        Arrays.fill(nums, 50, 80, 2);
        Arrays.fill(nums, 80, 100, 3);
        int[] result = topKFrequentElements.topKFrequentUsingHeap(nums, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void testTwoElements() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingHeap(new int[]{1, 2}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void testInitialStateUsingBucket() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{}, K);
        assertArrayEquals(new int[K], result);
    }

    @Test
    void testSingleHitUsingBucket() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 1, 1, 2, 2, 3}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void testSingleElementUsingBucket() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{5}, K);
        assertArrayEquals(new int[]{5}, result);
    }

    @Test
    void testAllSameElementUsingBucket() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{7, 7, 7, 7, 7}, K);
        assertArrayEquals(new int[]{7}, result);
    }

    @Test
    void testKEqualsOneUsingBucket() {
        int K = 1;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 2, 3, 3, 3, 4, 4}, K);
        assertArrayEquals(new int[]{3}, result);
    }

    @Test
    void testKEqualsUniqueElementsUsingBucket() {
        int K = 3;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 2, 3}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2, 3}, result);
    }

    @Test
    void testKGreaterThanUniqueElementsUsingBucket() {
        int K = 5;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 1, 2}, K);
        assertEquals(K, result.length);
    }

    @Test
    void testMultipleElementsWithDifferentFrequenciesUsingBucket() {
        int K = 3;
        int[] result =
                topKFrequentElements.topKFrequentUsingBucket(new int[]{4, 4, 4, 4, 3, 3, 3, 2, 2, 1}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{2, 3, 4}, result);
    }

    @Test
    void testAllElementsSameFrequencyUsingBucket() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 2, 3, 4}, K);
        assertEquals(K, result.length);
    }

    @Test
    void testLargeArrayUsingBucket() {
        int K = 2;
        int[] nums = new int[100];
        Arrays.fill(nums, 0, 50, 1);
        Arrays.fill(nums, 50, 80, 2);
        Arrays.fill(nums, 80, 100, 3);
        int[] result = topKFrequentElements.topKFrequentUsingBucket(nums, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }

    @Test
    void testTwoElementsUsingBucket() {
        int K = 2;
        int[] result = topKFrequentElements.topKFrequentUsingBucket(new int[]{1, 2}, K);
        Arrays.sort(result);
        assertArrayEquals(new int[]{1, 2}, result);
    }
}
