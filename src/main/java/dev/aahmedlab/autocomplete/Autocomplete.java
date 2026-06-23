package dev.aahmedlab.autocomplete;

import java.util.HashMap;
import java.util.Map;

public class Autocomplete {
    private final TrieNode root;
    private final int k;
    private final Map<String, Integer> freq;

    public Autocomplete(int k) {
        if (k <= 0) throw new IllegalArgumentException("k must be positive");
        this.k = k;
        this.freq = new HashMap<>();
        this.root = new TrieNode(k);
    }
}
