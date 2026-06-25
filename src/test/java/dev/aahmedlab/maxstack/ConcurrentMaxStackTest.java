package dev.aahmedlab.maxstack;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

class ConcurrentMaxStackTest {

  @Test
  void testPushAndTop() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    stack.push(5);
    stack.push(10);
    stack.push(3);

    assertEquals(3, stack.top(), "top returns the last pushed value");
  }

  @Test
  void testGetMaxTracksMaximum() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    stack.push(5);
    stack.push(10);
    stack.push(3);

    assertEquals(10, stack.getMax());
    assertEquals(3, stack.top());
  }

  @Test
  void testPopUpdatesMax() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    stack.push(5);
    stack.push(10);
    stack.push(3);

    assertEquals(3, stack.pop());
    assertEquals(10, stack.getMax(), "Max unchanged after popping a non-max element");

    assertEquals(10, stack.pop());
    assertEquals(5, stack.getMax(), "Max drops to next-largest after popping the max");
  }

  @Test
  void testPopMaxRemovesMaximumOnly() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    stack.push(5);
    stack.push(10);
    stack.push(3);

    assertEquals(10, stack.popMax(), "popMax returns the maximum");
    assertEquals(3, stack.top(), "top is preserved when the max is in the middle");
    assertEquals(5, stack.getMax());
  }

  @Test
  void testDuplicateMaxValues() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    stack.push(7);
    stack.push(7);
    stack.push(1);

    assertEquals(7, stack.getMax());
    assertEquals(7, stack.popMax());
    assertEquals(7, stack.getMax(), "Second 7 still present after removing one");
  }

  @Test
  void testEmptyStackReturnsNull() {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    assertNull(stack.pop());
    assertNull(stack.top());
    assertNull(stack.getMax());
    assertNull(stack.popMax());
  }

  @RepeatedTest(5)
  void testConcurrentPushPopBalances() throws InterruptedException {
    ConcurrentMaxStack<Integer> stack = new ConcurrentMaxStack<>();
    int numThreads = 12;
    int perThread = 500;

    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(numThreads);
    AtomicInteger popped = new AtomicInteger();

    for (int t = 0; t < numThreads; t++) {
      executor.submit(
          () -> {
            try {
              start.await();
              for (int i = 0; i < perThread; i++) {
                stack.push(i);
                if (stack.pop() != null) popped.incrementAndGet();
              }
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              done.countDown();
            }
          });
    }

    start.countDown();
    assertTrue(done.await(10, TimeUnit.SECONDS), "Workers should finish");
    executor.shutdown();

    // Equal pushes and pops per iteration: every pop must have seen an element, and the stack
    // must end empty with no lost-update corruption.
    assertEquals(numThreads * perThread, popped.get(), "Every pop should observe an element");
    assertNull(stack.top(), "Balanced push/pop leaves the stack empty");
    assertNull(stack.getMax());
  }
}
