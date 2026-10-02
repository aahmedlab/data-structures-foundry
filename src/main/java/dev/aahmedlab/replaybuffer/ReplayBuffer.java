package dev.aahmedlab.replaybuffer;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

record Message(long sequence, String payload) {}

class ReplayBuffer {
  private final Message[] buffer;
  private final int capacity;
  private long oldestRetainedSequence;
  private long latestSequence;

  public ReplayBuffer(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException("capacity must be positive");
    }
    this.capacity = capacity;
    buffer = new Message[this.capacity];
    oldestRetainedSequence = -1;
    latestSequence = -1;
  }

  public void append(long sequence, String payload) {
    if (sequence < 0) {
      throw new IllegalArgumentException("sequence must be nonnegative");
    }

    if (payload == null) {
      throw new IllegalArgumentException("payload must not be null");
    }

    boolean bufferIsNotEmpty = latestSequence != -1;

    if (bufferIsNotEmpty && sequence != latestSequence + 1) {
      throw new IllegalArgumentException("sequence must follow the latest sequence");
    }

    int index = Math.floorMod(sequence, this.capacity);

    if (buffer[index] != null) {
      oldestRetainedSequence++;
    } else if (buffer[index] == null && oldestRetainedSequence == -1) {
      oldestRetainedSequence = sequence;
    }

    buffer[index] = new Message(sequence, payload);
    latestSequence = sequence;
  }

  public List<Message> replay(long fromInclusive, long toInclusive) {
    if (fromInclusive > toInclusive) {
      throw new IllegalArgumentException("invalid range");
    }

    boolean unavailable =
        latestSequence == -1
            || fromInclusive < oldestRetainedSequence
            || toInclusive > latestSequence;

    if (unavailable) {
      throw new NoSuchElementException("requested range is unavailable");
    }

    List<Message> replayMessages = new ArrayList<>();

    long n = toInclusive - fromInclusive + 1;
    int startIndex = Math.floorMod(fromInclusive, this.capacity);
    for (int i = 0; i < n; i++) {
      int circularIndex = Math.floorMod(startIndex + (long) i, this.capacity);
      replayMessages.add(buffer[circularIndex]);
    }

    return replayMessages;
  }
}
