package dev.aahmedlab.ratelimiter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TokenBucketTest {

    private TokenBucket tokenBucket;

    @BeforeEach
    void setUp() {
        tokenBucket = new TokenBucket(10, 5);
    }

    @Test
    void testInitialState() {
        TokenBucket bucket = new TokenBucket(10, 5);
        assertTrue(bucket.allowRequest());
    }

    @Test
    void testConsumeAllTokens() {
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());
    }

    @Test
    void testRefillOverTime() throws InterruptedException {
        // Consume all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());

        // Wait for refill (5 tokens per second, wait 1 second = 5 tokens)
        Thread.sleep(1000);

        // Should have 5 tokens now
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());
    }

    @Test
    void testPartialRefill() throws InterruptedException {
        // Consume all tokens
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());

        // Wait for partial refill (5 tokens per second, wait 200ms = 1 token)
        Thread.sleep(200);

        assertTrue(tokenBucket.allowRequest());
        assertFalse(tokenBucket.allowRequest());
    }

    @Test
    void testBucketNeverExceedsCapacity() throws InterruptedException {
        // Consume some tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
        }

        // Wait long enough to refill all tokens and more
        Thread.sleep(3000);

        // Should still only have capacity tokens available
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());
    }

    @Test
    void testZeroRefillRate() {
        TokenBucket bucket = new TokenBucket(5, 0);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.allowRequest());
        }
        assertFalse(bucket.allowRequest());
    }

    @Test
    void testCapacityOne() {
        TokenBucket bucket = new TokenBucket(1, 1);
        assertTrue(bucket.allowRequest());
        assertFalse(bucket.allowRequest());
    }

    @Test
    void testHighRefillRate() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(5, 100);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.allowRequest());
        }
        assertFalse(bucket.allowRequest());

        // Wait 100ms for refill (100 tokens/sec * 0.1s = 10 tokens, but capped at 5)
        Thread.sleep(100);

        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.allowRequest());
        }
        assertFalse(bucket.allowRequest());
    }

    @ParameterizedTest
    @CsvSource({"5, 1, 5", "10, 5, 10", "20, 10, 20", "100, 50, 100"})
    void testDifferentCapacitiesAndRefillRates(
            int capacity, int refillRate, int expectedInitialTokens) {
        TokenBucket bucket = new TokenBucket(capacity, refillRate);
        int allowedCount = 0;
        for (int i = 0; i < expectedInitialTokens + 1; i++) {
            if (bucket.allowRequest()) {
                allowedCount++;
            }
        }
        assertEquals(expectedInitialTokens, allowedCount);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 50, 100})
    void testVariousCapacities(int capacity) {
        TokenBucket bucket = new TokenBucket(capacity, capacity);
        int allowedCount = 0;
        for (int i = 0; i < capacity + 1; i++) {
            if (bucket.allowRequest()) {
                allowedCount++;
            }
        }
        assertEquals(capacity, allowedCount);
    }

    @Test
    void testImmediateRequests() {
        int allowedCount = 0;
        for (int i = 0; i < 15; i++) {
            if (tokenBucket.allowRequest()) {
                allowedCount++;
            }
        }
        assertEquals(10, allowedCount);
    }

    @Test
    void testGradualConsumption() throws InterruptedException {
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
            Thread.sleep(50);
        }
        // Should still have tokens due to refill during consumption
        assertTrue(tokenBucket.allowRequest());
    }

    @Test
    void testEmptyBucketBehavior() {
        TokenBucket bucket = new TokenBucket(5, 1);
        for (int i = 0; i < 5; i++) {
            assertTrue(bucket.allowRequest());
        }
        // Multiple requests on empty bucket should all fail
        for (int i = 0; i < 10; i++) {
            assertFalse(bucket.allowRequest());
        }
    }

    @Test
    void testRefillAfterPartialConsumption() throws InterruptedException {
        // Consume half the tokens
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
        }

        // Wait for refill
        Thread.sleep(1000);

        // Should have refilled to capacity
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());
    }

    @Test
    void testSmallRefillRate() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(10, 1);
        for (int i = 0; i < 10; i++) {
            assertTrue(bucket.allowRequest());
        }
        assertFalse(bucket.allowRequest());

        // Wait 1 second for 1 token
        Thread.sleep(1000);
        assertTrue(bucket.allowRequest());
        assertFalse(bucket.allowRequest());
    }

    @Test
    void testLargeCapacity() {
        TokenBucket bucket = new TokenBucket(1000, 100);
        int allowedCount = 0;
        for (int i = 0; i < 1000; i++) {
            if (bucket.allowRequest()) {
                allowedCount++;
            }
        }
        assertEquals(1000, allowedCount);
    }

    @Test
    void testFractionalTokenAccumulation() throws InterruptedException {
        TokenBucket bucket = new TokenBucket(10, 1);
        for (int i = 0; i < 10; i++) {
            assertTrue(bucket.allowRequest());
        }
        assertFalse(bucket.allowRequest());

        // Wait 500ms for 0.5 tokens (not enough for a request)
        Thread.sleep(500);
        assertFalse(bucket.allowRequest());

        // Wait another 500ms for another 0.5 tokens (total 1.0)
        Thread.sleep(500);
        assertTrue(bucket.allowRequest());
    }

    @Test
    void testMultipleRefillCycles() throws InterruptedException {
        for (int i = 0; i < 10; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());

        // First refill cycle
        Thread.sleep(1000);
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());

        // Second refill cycle
        Thread.sleep(1000);
        for (int i = 0; i < 5; i++) {
            assertTrue(tokenBucket.allowRequest());
        }
        assertFalse(tokenBucket.allowRequest());
    }
}
