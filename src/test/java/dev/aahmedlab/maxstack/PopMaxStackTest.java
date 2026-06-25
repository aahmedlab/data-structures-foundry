package dev.aahmedlab.maxstack;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PopMaxStackTest {

    @Test
    void testPushAndTop() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(3, stack.top(), "Peek should return top element");
    }

    @Test
    void testGetMaxSingleElement() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);

        assertEquals(5, stack.getMax(), "Max should be the only element");
        assertEquals(5, stack.top(), "Peek should return the same element");
    }

    @Test
    void testGetMaxMultipleElements() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);
        stack.push(7);

        assertEquals(10, stack.getMax(), "Max should be 10");
        assertEquals(7, stack.top(), "Peek should return top element (7)");
    }

    @Test
    void testPopUpdatesMax() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
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
        PopMaxStack<Integer> stack = new PopMaxStack<>();
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
        PopMaxStack<Integer> stack = new PopMaxStack<>();

        assertNull(stack.top(), "Peek on empty stack should return null");
        assertNull(stack.pop(), "Pop on empty stack should return null");
        assertNull(stack.getMax(), "getMax on empty stack should return null");
        assertNull(stack.popMax(), "popMax on empty stack should return null");
    }

    @Test
    void testPopMaxBasic() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(10, stack.popMax(), "popMax should return 10");
        assertEquals(3, stack.top(), "After popMax, top should still be 3");
        assertEquals(5, stack.getMax(), "After removing 10, max should be 5");
    }

    @Test
    void testPopMaxSingleElement() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);

        assertEquals(5, stack.popMax(), "popMax should return the only element");
        assertNull(stack.top(), "Stack should be empty after popMax");
        assertNull(stack.getMax(), "getMax should return null after stack is empty");
    }

    @Test
    void testPopMaxTopElement() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);

        assertEquals(10, stack.popMax(), "popMax should return 10 (top element)");
        assertEquals(5, stack.top(), "Peek should return 5");
        assertEquals(5, stack.getMax(), "Max should be 5");
    }

    @Test
    void testPopMaxMiddleElement() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);
        stack.push(7);

        assertEquals(10, stack.popMax(), "popMax should return 10 from middle");
        assertEquals(7, stack.top(), "Top should still be 7");
        assertEquals(7, stack.getMax(), "Max should now be 7");

        assertEquals(7, stack.pop(), "Pop should return 7");
        assertEquals(3, stack.pop(), "Pop should return 3");
        assertEquals(5, stack.pop(), "Pop should return 5");
    }

    @Test
    void testPopMaxBottomElement() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(10);
        stack.push(5);
        stack.push(3);

        assertEquals(10, stack.popMax(), "popMax should return 10 from bottom");
        assertEquals(3, stack.top(), "Top should still be 3");
        assertEquals(5, stack.getMax(), "Max should now be 5");
    }

    @Test
    void testDuplicateValuesPopMax() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(10);
        stack.push(3);

        assertEquals(10, stack.getMax(), "Max should be 10");
        assertEquals(10, stack.popMax(), "popMax should return 10 (most recent)");
        assertEquals(10, stack.getMax(), "Max should still be 10 (duplicate exists)");
        assertEquals(10, stack.popMax(), "popMax should return second 10");
        assertEquals(5, stack.getMax(), "Max should now be 5");
    }

    @Test
    void testAlternatingPopAndPopMax() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);
        stack.push(8);
        stack.push(1);

        assertEquals(1, stack.pop(), "Pop should return 1");
        assertEquals(10, stack.popMax(), "popMax should return 10");
        assertEquals(8, stack.pop(), "Pop should return 8");
        assertEquals(5, stack.getMax(), "Max should now be 5");
        assertEquals(5, stack.popMax(), "popMax should return 5");
        assertEquals(3, stack.top(), "Only 3 should remain");
    }

    @Test
    void testPopMaxWithAllSameValues() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(5);
        stack.push(5);

        assertEquals(5, stack.popMax(), "popMax should return 5");
        assertEquals(5, stack.getMax(), "Max should still be 5");
        assertEquals(5, stack.popMax(), "popMax should return 5");
        assertEquals(5, stack.popMax(), "popMax should return last 5");
        assertNull(stack.getMax(), "Stack should be empty");
    }

    @Test
    void testIncreasingSequence() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        stack.push(4);
        stack.push(5);

        assertEquals(5, stack.getMax(), "Max should be 5");
        assertEquals(5, stack.top(), "Peek should be 5");

        stack.pop();
        assertEquals(4, stack.getMax(), "Max should be 4 after popping 5");
    }

    @Test
    void testDecreasingSequence() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(4);
        stack.push(3);
        stack.push(2);
        stack.push(1);

        assertEquals(5, stack.getMax(), "Max should be 5");
        assertEquals(1, stack.top(), "Peek should be 1");

        stack.pop();
        assertEquals(5, stack.getMax(), "Max should still be 5");
    }

    @Test
    void testWithStrings() {
        PopMaxStack<String> stack = new PopMaxStack<>();
        stack.push("apple");
        stack.push("zebra");
        stack.push("banana");
        stack.push("mango");

        assertEquals("zebra", stack.getMax(), "Max should be 'zebra' (lexicographically)");
        assertEquals("mango", stack.top(), "Peek should be 'mango'");

        assertEquals("zebra", stack.popMax(), "popMax should return 'zebra'");
        assertEquals("mango", stack.top(), "Peek should still be 'mango'");
        assertEquals("mango", stack.getMax(), "Max should now be 'mango'");
    }

    @Test
    void testNegativeNumbers() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(-10);
        stack.push(-5);
        stack.push(-20);
        stack.push(-3);

        assertEquals(-3, stack.getMax(), "Max should be -3");
        assertEquals(-3, stack.top(), "Peek should be -3");

        stack.pop();
        assertEquals(-5, stack.getMax(), "Max should be -5 after removing -3");

        assertEquals(-5, stack.popMax(), "popMax should return -5");
        assertEquals(-10, stack.getMax(), "Max should be -10");
    }

    @Test
    void testMixedPositiveNegative() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(-5);
        stack.push(10);
        stack.push(-3);
        stack.push(0);
        stack.push(7);

        assertEquals(10, stack.getMax(), "Max should be 10");
        assertEquals(10, stack.popMax(), "popMax should return 10");
        assertEquals(7, stack.getMax(), "Max should be 7");
        assertEquals(7, stack.top(), "Peek should be 7");
    }

    @Test
    void testLargeDataSet() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        int maxValue = 100;

        for (int i = 0; i <= maxValue; i++) {
            stack.push(i);
        }

        assertEquals(maxValue, stack.getMax(), "Max should be " + maxValue);
        assertEquals(maxValue, stack.top(), "Peek should be " + maxValue);

        assertEquals(maxValue, stack.popMax(), "popMax should return " + maxValue);
        assertEquals(maxValue - 1, stack.getMax(), "Max should be " + (maxValue - 1));
        assertEquals(maxValue - 1, stack.top(), "Peek should be " + (maxValue - 1));
    }

    @Test
    void testComplexSequence() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(5);
        stack.push(10);
        stack.push(3);

        assertEquals(10, stack.popMax(), "popMax should return 10");

        stack.push(15);
        stack.push(2);

        assertEquals(15, stack.getMax(), "Max should be 15");
        assertEquals(2, stack.top(), "Peek should be 2");

        stack.pop(); // Remove 2
        assertEquals(15, stack.popMax(), "popMax should return 15");
        assertEquals(3, stack.top(), "Peek should be 3");
        assertEquals(5, stack.getMax(), "Max should be 5");
    }

    @Test
    void testPopMaxMultipleTimes() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(1);
        stack.push(5);
        stack.push(3);
        stack.push(10);
        stack.push(7);

        assertEquals(10, stack.popMax(), "First popMax should return 10");
        assertEquals(7, stack.popMax(), "Second popMax should return 7");
        assertEquals(5, stack.popMax(), "Third popMax should return 5");
        assertEquals(3, stack.popMax(), "Fourth popMax should return 3");
        assertEquals(1, stack.popMax(), "Fifth popMax should return 1");
        assertNull(stack.popMax(), "popMax on empty stack should return null");
    }

    @Test
    void testPopMaxDoesNotAffectStackOrder() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(1);
        stack.push(10);
        stack.push(2);
        stack.push(3);

        assertEquals(10, stack.popMax(), "popMax should return 10");

        assertEquals(3, stack.pop(), "Pop should return 3");
        assertEquals(2, stack.pop(), "Pop should return 2");
        assertEquals(1, stack.pop(), "Pop should return 1");
        assertNull(stack.pop(), "Stack should be empty");
    }

    @Test
    void testZeroValue() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(0);
        stack.push(-1);
        stack.push(1);

        assertEquals(1, stack.getMax(), "Max should be 1");
        assertEquals(1, stack.popMax(), "popMax should return 1");
        assertEquals(0, stack.getMax(), "Max should be 0");
        assertEquals(-1, stack.top(), "Peek should be -1");
    }

    @Test
    void testDuplicatesInDifferentPositions() {
        PopMaxStack<Integer> stack = new PopMaxStack<>();
        stack.push(10);
        stack.push(5);
        stack.push(10);
        stack.push(3);
        stack.push(10);

        assertEquals(10, stack.popMax(), "popMax should return 10 (most recent)");
        assertEquals(3, stack.top(), "Peek should still be 3");
        assertEquals(10, stack.getMax(), "Max should still be 10");

        assertEquals(10, stack.popMax(), "popMax should return 10 (middle)");
        assertEquals(10, stack.getMax(), "Max should still be 10 (one more left)");

        assertEquals(10, stack.popMax(), "popMax should return 10 (bottom)");
        assertEquals(5, stack.getMax(), "Max should now be 5");
    }
}
