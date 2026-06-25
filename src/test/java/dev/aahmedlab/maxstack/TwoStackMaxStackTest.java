package dev.aahmedlab.maxstack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TwoStackMaxStackTest {

    @Test
    void testPushAndPeek() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(3, stack.peek(), "Peek should return top element");
    }

    @Test
    void testGetMaxSingleElement() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);

        assertEquals(5, stack.getMax(), "Max should be the only element");
        assertEquals(5, stack.peek(), "Peek should return the same element");
    }

    @Test
    void testGetMaxMultipleElements() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);
        stack.push(7);

        assertEquals(10, stack.getMax(), "Max should be 10");
        assertEquals(7, stack.peek(), "Peek should return top element (7)");
    }

    @Test
    void testPopUpdatesMax() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(10, stack.getMax(), "Max should be 10");

        stack.pop(); // Remove 3
        assertEquals(10, stack.getMax(), "Max should still be 10");

        stack.pop(); // Remove 10 (the max)
        assertEquals(5, stack.getMax(), "Max should now be 5");
    }

    @Test
    void testPopAllElements() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(3, stack.pop(), "First pop should return 3");
        assertEquals(10, stack.pop(), "Second pop should return 10");
        assertEquals(5, stack.pop(), "Third pop should return 5");
        assertNull(stack.pop(), "Pop from empty stack should return null");
    }

    @Test
    void testEmptyStack() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();

        assertNull(stack.peek(), "Peek on empty stack should return null");
        assertNull(stack.pop(), "Pop on empty stack should return null");
        assertNull(stack.getMax(), "getMax on empty stack should return null");
    }

    @Test
    void testDuplicateValues() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(10);
        stack.push(3);

        assertEquals(10, stack.getMax(), "Max should be 10");

        stack.pop(); // Remove 3
        assertEquals(10, stack.getMax(), "Max should still be 10");

        stack.pop(); // Remove one 10
        assertEquals(10, stack.getMax(), "Max should still be 10 (duplicate)");

        stack.pop(); // Remove second 10
        assertEquals(5, stack.getMax(), "Max should now be 5");
    }

    @Test
    void testIncreasingSequence() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        stack.push(5);

        assertEquals(5, stack.getMax(), "Max should be 5");
        assertEquals(5, stack.peek(), "Peek should be 5");

        stack.pop();
        assertEquals(4, stack.getMax(), "Max should be 4 after popping 5");
    }

    @Test
    void testDecreasingSequence() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(4);
        stack.push(3);
        stack.push(2);
        stack.push(1);

        assertEquals(5, stack.getMax(), "Max should be 5");
        assertEquals(1, stack.peek(), "Peek should be 1");

        stack.pop();
        assertEquals(5, stack.getMax(), "Max should still be 5");
    }

    @Test
    void testWithStrings() {
        TwoStackMaxStack<String> stack = new TwoStackMaxStack<>();
        stack.push("apple");
        stack.push("zebra");
        stack.push("banana");
        stack.push("mango");

        assertEquals("zebra", stack.getMax(), "Max should be 'zebra' (lexicographically)");
        assertEquals("mango", stack.peek(), "Peek should be 'mango'");

        stack.pop(); // Remove mango
        assertEquals("zebra", stack.getMax(), "Max should still be 'zebra'");

        stack.pop(); // Remove banana
        stack.pop(); // Remove zebra
        assertEquals("apple", stack.getMax(), "Max should now be 'apple'");
    }

    @Test
    void testNegativeNumbers() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(-10);
        stack.push(-5);
        stack.push(-20);
        stack.push(-3);

        assertEquals(-3, stack.getMax(), "Max should be -3");
        assertEquals(-3, stack.peek(), "Peek should be -3");

        stack.pop();
        assertEquals(-5, stack.getMax(), "Max should be -5 after removing -3");
    }

    @Test
    void testMixedPositiveNegative() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(-5);
        stack.push(10);
        stack.push(-3);
        stack.push(0);
        stack.push(7);

        assertEquals(10, stack.getMax(), "Max should be 10");

        stack.pop(); // Remove 7
        stack.pop(); // Remove 0
        stack.pop(); // Remove -3
        assertEquals(10, stack.getMax(), "Max should still be 10");

        stack.pop(); // Remove 10
        assertEquals(-5, stack.getMax(), "Max should now be -5");
    }

    @Test
    void testSameValueMultipleTimes() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        stack.push(5);
        stack.push(5);
        stack.push(5);

        assertEquals(5, stack.getMax(), "Max should be 5");

        for (int i = 0; i < 4; i++) {
            assertEquals(5, stack.pop(), "Each pop should return 5");
            if (i < 3) {
                assertEquals(5, stack.getMax(), "Max should remain 5");
            }
        }

        assertNull(stack.getMax(), "Max should be null after all elements removed");
    }

    @Test
    void testLargeDataSet() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        int maxValue = 100;

        for (int i = 0; i <= maxValue; i++) {
            stack.push(i);
        }

        assertEquals(maxValue, stack.getMax(), "Max should be " + maxValue);
        assertEquals(maxValue, stack.peek(), "Peek should be " + maxValue);

        for (int i = maxValue; i >= 0; i--) {
            assertEquals(i, stack.getMax(), "Max should be " + i);
            assertEquals(i, stack.pop(), "Pop should return " + i);
        }

        assertNull(stack.pop(), "Stack should be empty");
    }

    @Test
    void testPushPopSequence() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(5);
        assertEquals(5, stack.getMax(), "Max should be 5");

        stack.push(10);
        assertEquals(10, stack.getMax(), "Max should be 10");

        stack.pop();
        assertEquals(5, stack.getMax(), "Max should be 5 after pop");

        stack.push(3);
        assertEquals(5, stack.getMax(), "Max should still be 5");

        stack.push(8);
        assertEquals(8, stack.getMax(), "Max should be 8");

        stack.pop();
        assertEquals(5, stack.getMax(), "Max should be 5 after pop");
    }

    @Test
    void testZeroValue() {
        TwoStackMaxStack<Integer> stack = new TwoStackMaxStack<>();
        stack.push(0);
        stack.push(-1);
        stack.push(1);

        assertEquals(1, stack.getMax(), "Max should be 1");
        stack.pop();
        assertEquals(0, stack.getMax(), "Max should be 0");
        stack.pop();
        assertEquals(0, stack.getMax(), "Max should still be 0");
    }
}
