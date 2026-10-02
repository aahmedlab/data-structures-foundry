package dev.aahmedlab.replaybuffer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ReplayBufferTest {

  private static final int CAPACITY = 3;
  private ReplayBuffer buffer;

  @BeforeEach
  void setUp() {
    buffer = new ReplayBuffer(CAPACITY);
  }

  /** Appends payloads "p<seq>" for every sequence in [from, to]. */
  private void appendRange(ReplayBuffer target, long from, long to) {
    for (long sequence = from; sequence <= to; sequence++) {
      target.append(sequence, "p" + sequence);
    }
  }

  /** Asserts the replayed messages carry exactly the sequences in [from, to], in order. */
  private void assertReplayIs(List<Message> messages, long from, long to) {
    assertEquals(to - from + 1, messages.size());
    for (int i = 0; i < messages.size(); i++) {
      long expectedSequence = from + i;
      assertEquals(expectedSequence, messages.get(i).sequence());
      assertEquals("p" + expectedSequence, messages.get(i).payload());
    }
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -10, Integer.MIN_VALUE})
  void testConstructorRejectsNonPositiveCapacity(int capacity) {
    assertThrows(IllegalArgumentException.class, () -> new ReplayBuffer(capacity));
  }

  @ParameterizedTest
  @ValueSource(ints = {1, 2, 16, 1024})
  void testConstructorAcceptsPositiveCapacity(int capacity) {
    assertDoesNotThrow(() -> new ReplayBuffer(capacity));
  }

  @Test
  void testReplayOnEmptyBuffer() {
    assertThrows(NoSuchElementException.class, () -> buffer.replay(0, 0));
  }

  @ParameterizedTest
  @ValueSource(longs = {-1, -5, Long.MIN_VALUE})
  void testAppendRejectsNegativeSequence(long sequence) {
    assertThrows(IllegalArgumentException.class, () -> buffer.append(sequence, "payload"));
  }

  @Test
  void testAppendRejectsNullPayload() {
    assertThrows(IllegalArgumentException.class, () -> buffer.append(0, null));
  }

  @Test
  void testAppendRejectsGapInSequence() {
    buffer.append(0, "p0");
    assertThrows(IllegalArgumentException.class, () -> buffer.append(2, "p2"));
  }

  @Test
  void testAppendRejectsRepeatedSequence() {
    buffer.append(0, "p0");
    assertThrows(IllegalArgumentException.class, () -> buffer.append(0, "duplicate"));
  }

  @Test
  void testAppendRejectsOutOfOrderSequence() {
    appendRange(buffer, 0, 2);
    assertThrows(IllegalArgumentException.class, () -> buffer.append(1, "p1"));
  }

  @Test
  void testSingleAppendAndReplay() {
    buffer.append(0, "p0");
    assertReplayIs(buffer.replay(0, 0), 0, 0);
  }

  @Test
  void testFirstAppendMayStartAtAnySequence() {
    buffer.append(100, "p100");
    assertReplayIs(buffer.replay(100, 100), 100, 100);
    assertThrows(NoSuchElementException.class, () -> buffer.replay(99, 100));
  }

  @Test
  void testReplayFullBufferWithoutEviction() {
    appendRange(buffer, 0, 2);
    assertReplayIs(buffer.replay(0, 2), 0, 2);
  }

  @ParameterizedTest
  @CsvSource({"0,0", "0,1", "0,2", "1,1", "1,2", "2,2"})
  void testReplaySubRanges(long from, long to) {
    appendRange(buffer, 0, 2);
    assertReplayIs(buffer.replay(from, to), from, to);
  }

  @Test
  void testReplayRejectsInvertedRange() {
    appendRange(buffer, 0, 2);
    assertThrows(IllegalArgumentException.class, () -> buffer.replay(2, 1));
  }

  @Test
  void testInvertedRangeIsRejectedBeforeAvailability() {
    assertThrows(IllegalArgumentException.class, () -> buffer.replay(50, 10));
  }

  @Test
  void testReplayBeyondLatestSequence() {
    appendRange(buffer, 0, 2);
    assertThrows(NoSuchElementException.class, () -> buffer.replay(2, 3));
    assertThrows(NoSuchElementException.class, () -> buffer.replay(3, 3));
  }

  @Test
  void testReplayBeforeOldestRetainedSequence() {
    appendRange(buffer, 0, 2);
    assertThrows(NoSuchElementException.class, () -> buffer.replay(-1, 1));
  }

  @Test
  void testEvictionAdvancesOldestRetainedSequence() {
    appendRange(buffer, 0, 3); // sequence 0 is evicted by sequence 3

    assertThrows(NoSuchElementException.class, () -> buffer.replay(0, 3));
    assertReplayIs(buffer.replay(1, 3), 1, 3);
  }

  @Test
  void testRepeatedEvictionKeepsOnlyTheLastCapacityMessages() {
    appendRange(buffer, 0, 9); // 10 appends into a buffer of 3

    assertThrows(NoSuchElementException.class, () -> buffer.replay(6, 9));
    assertReplayIs(buffer.replay(7, 9), 7, 9);
  }

  @Test
  void testReplayWrapsAroundTheBackingArray() {
    appendRange(buffer, 0, 4); // retained: 2, 3, 4 stored at indices 2, 0, 1

    assertReplayIs(buffer.replay(2, 4), 2, 4);
    assertReplayIs(buffer.replay(2, 3), 2, 3);
    assertReplayIs(buffer.replay(3, 4), 3, 4);
  }

  @Test
  void testEvictionWithNonZeroStartingSequence() {
    buffer.append(5, "p5"); // index 2
    appendRange(buffer, 6, 8); // indices 0, 1, 2 - sequence 5 evicted by 8

    assertThrows(NoSuchElementException.class, () -> buffer.replay(5, 8));
    assertReplayIs(buffer.replay(6, 8), 6, 8);
  }

  @Test
  void testCapacityOneRetainsOnlyTheLatestMessage() {
    ReplayBuffer single = new ReplayBuffer(1);
    appendRange(single, 0, 2);

    assertReplayIs(single.replay(2, 2), 2, 2);
    assertThrows(NoSuchElementException.class, () -> single.replay(1, 2));
    assertThrows(NoSuchElementException.class, () -> single.replay(1, 1));
  }

  @Test
  void testReplayIsNonDestructive() {
    appendRange(buffer, 0, 4);

    assertReplayIs(buffer.replay(2, 4), 2, 4);
    assertReplayIs(buffer.replay(2, 4), 2, 4);
    assertReplayIs(buffer.replay(3, 3), 3, 3);
  }

  @Test
  void testAppendAfterReplayContinuesTheSequence() {
    appendRange(buffer, 0, 2);
    assertReplayIs(buffer.replay(0, 2), 0, 2);

    buffer.append(3, "p3");
    assertReplayIs(buffer.replay(1, 3), 1, 3);
  }

  @Test
  void testRejectedAppendLeavesBufferUnchanged() {
    appendRange(buffer, 0, 2);

    assertThrows(IllegalArgumentException.class, () -> buffer.append(5, "p5"));
    assertThrows(IllegalArgumentException.class, () -> buffer.append(3, null));

    assertReplayIs(buffer.replay(0, 2), 0, 2);
    buffer.append(3, "p3"); // sequence 3 is still the expected next one
    assertReplayIs(buffer.replay(1, 3), 1, 3);
  }

  @Test
  void testLargeSequenceNumbersWrapCorrectly() {
    ReplayBuffer large = new ReplayBuffer(4);
    long start = 1_000_000_001L;
    appendRange(large, start, start + 9);

    assertReplayIs(large.replay(start + 6, start + 9), start + 6, start + 9);
    assertThrows(NoSuchElementException.class, () -> large.replay(start + 5, start + 9));
  }

  @Test
  void testMessageEqualityAndAccessors() {
    buffer.append(7, "hello");
    Message message = buffer.replay(7, 7).get(0);

    assertEquals(new Message(7, "hello"), message);
    assertEquals(7, message.sequence());
    assertEquals("hello", message.payload());
  }
}
