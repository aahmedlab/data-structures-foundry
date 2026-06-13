package dev.aahmedlab.maxstack;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A single-threaded max stack supporting O(1) push, pop, peek, and max retrieval, implemented with
 * the classic two-stack technique.
 *
 * <p>This class maintains two coordinated data structures:
 *
 * <ul>
 *   <li>A main {@link Deque} holding all elements in stack order
 *   <li>An auxiliary {@link Deque} whose top always mirrors the current maximum
 * </ul>
 *
 * <p><b>Design Decisions:</b>
 *
 * <ul>
 *   <li><b>Two-stack technique:</b> A value is pushed onto the max stack only when it is greater
 *       than or equal to the current max, so the max stack's top always tracks the running maximum
 *       in O(1).
 *   <li><b>Duplicate handling via {@code >=}:</b> Equal maxima are pushed again, so popping one
 *       duplicate does not lose the max for the remaining occurrences.
 *   <li><b>No {@code popMax}:</b> Removing the max from the middle of the stack cannot be done
 *       efficiently with this representation; see {@link PopMaxStack} for a variant that supports
 *       {@code popMax}.
 * </ul>
 *
 * <p><b>Thread Safety:</b> This class is <em>not</em> thread-safe. See {@link ConcurrentMaxStack}
 * for a concurrent variant.
 *
 * @param <T> the type of elements in this stack, must be {@link Comparable}
 */
class TwoStackMaxStack<T extends Comparable<T>> {
  private Deque<T> mainStack = new ArrayDeque<>();
  private Deque<T> maxStack = new ArrayDeque<>();

  public void push(T value) {
    T currentMax = maxStack.peek();
    mainStack.push(value);
    if (currentMax == null || value.compareTo(currentMax) >= 0) {
      maxStack.push(value);
    }
  }

  public T pop() {
    T top = mainStack.peek();
    if (top == null) return null;
    T currentMax = maxStack.peek();
    if (currentMax != null && top.compareTo(currentMax) == 0) {
      maxStack.pop();
    }
    return mainStack.pop();
  }

  public T peek() {
    return mainStack.peek();
  }

  public T getMax() {
    return maxStack.peek();
  }
}
