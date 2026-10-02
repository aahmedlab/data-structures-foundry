package dev.aahmedlab.timemap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimeMap {
  private Map<String, List<Entry>> cache = new HashMap<>();

  public void set(String key, String value, int timestamp) {
    cache.computeIfAbsent(key, k -> new ArrayList<>()).add(new Entry(timestamp, value));
  }

  public String get(String key, int timestamp) {
    List<Entry> data = cache.get(key);

    if (data != null && !data.isEmpty()) {
      return search(data, timestamp);
    }
    return "";
  }

  private String search(List<Entry> data, int timestamp) {
    int left = 0;
    int right = data.size() - 1;
    int result = -1;

    while (left <= right) {
      int mid = left + (right - left) / 2;
      if (data.get(mid).timestamp() <= timestamp) {
        result = mid;
        left = mid + 1;
      } else {
        right = mid - 1;
      }
    }

    return result == -1 ? "" : data.get(result).value();
  }
}
