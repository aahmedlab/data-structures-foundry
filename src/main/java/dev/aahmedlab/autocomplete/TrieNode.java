package dev.aahmedlab.autocomplete;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class TrieNode {
    private final TrieNode[] children = new TrieNode[26];
    private final PriorityQueue<Entry> topK;
    private final int k;

    public TrieNode(int k){
        if (k <= 0) throw new IllegalArgumentException("`k` cannot be less than `0`.");
        this.k = k;
        topK = new PriorityQueue<>(k, Comparator.comparingInt(Entry::freq));
    }

    public void updateCache(final String word, final int newFreq){
        boolean existed = topK.removeIf(e -> e.word().equals(word));

        if (existed || topK.size() < k) {
            topK.offer(new Entry(word, newFreq));
        } else {
            Entry min = topK.peek();
            if (min != null && newFreq > min.freq()) {
                topK.poll();
                topK.offer(new Entry(word, newFreq));
            }
        }
    }

    public List<String> sorted(){
        List<Entry> entries = new ArrayList<>(topK);
        entries.sort(Comparator
                .comparingInt(Entry::freq)
                .reversed()
                .thenComparing(Entry::word));
        return entries.stream().map(Entry::word).toList();
    }
}
