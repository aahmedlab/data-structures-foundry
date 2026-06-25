package dev.aahmedlab.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class FixedWindowTest {

    private FixedWindow limiter;

    @BeforeEach
    void setUp() {
        limiter = new FixedWindow(10, 1000);
    }

    @Test
    void testConstructorRejectsInvalidCapacity() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindow(0, 1000));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindow(-1, 1000));
    }

    @Test
    void testConstructorRejectsInvalidWindow() {
        assertThrows(IllegalArgumentException.class, () -> new FixedWindow(10, 0));
        assertThrows(IllegalArgumentException.class, () -> new FixedWindow(10, -1));
    }

    @Test
    void testInitialState() {
        assertTrue(limiter.allowRequest());
    }

    @Test
    void testConsumeAllCapacity() {
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        assertFalse(limiter.allowRequest());
    }

    @Test
    void testCapacityOne() {
        FixedWindow single = new FixedWindow(1, 1000);
        assertTrue(single.allowRequest());
        assertFalse(single.allowRequest());
    }

    @Test
    void testWindowResetsOverTime() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        assertFalse(limiter.allowRequest());

        Thread.sleep(1100);

        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        assertFalse(limiter.allowRequest());
    }

    @Test
    void testPartialWindowDoesNotReset() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            assertTrue(limiter.allowRequest());
        }
        assertFalse(limiter.allowRequest());

        Thread.sleep(300);

        assertFalse(limiter.allowRequest());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 50, 100})
    void testVariousCapacities(int capacity) {
        FixedWindow l = new FixedWindow(capacity, 1000);
        int allowed = 0;
        for (int i = 0; i < capacity + 5; i++) {
            if (l.allowRequest()) {
                allowed++;
            }
        }
        assertEquals(capacity, allowed);
    }
}
