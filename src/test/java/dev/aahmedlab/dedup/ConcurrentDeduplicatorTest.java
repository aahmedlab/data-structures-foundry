package dev.aahmedlab.dedup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentDeduplicatorTest {

    private static final int TTL = 60;
    private ConcurrentDeduplicator<String> deduplicator;

    static class MutableClock extends Clock {
        private volatile Instant currentInstant;
        private final ZoneId zone;

        public MutableClock(Instant instant, ZoneId zone) {
            this.currentInstant = instant;
            this.zone = zone;
        }

        public void setInstant(Instant instant) {
            this.currentInstant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(currentInstant, zone);
        }

        @Override
        public Instant instant() {
            return currentInstant;
        }
    }

    @BeforeEach
    void setUp() {
        deduplicator = new ConcurrentDeduplicator<>(TTL);
    }

    @Test
    void testSingleHit() {
        assertTrue(deduplicator.isFirstSeen("k1"));
    }

    @Test
    void testSameKeyWithinTheTTLTime() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> testDedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(testDedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1010L));
        assertFalse(testDedup.isFirstSeen("k1"));
    }

    @Test
    void testKeyExpiresAfterTTL() {
        Instant startTime = Instant.ofEpochSecond(1000L);
        MutableClock clock = new MutableClock(startTime, ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        
        clock.setInstant(startTime.plusSeconds(60));
        assertFalse(dedup.isFirstSeen("k1"));
        
        clock.setInstant(startTime.plusSeconds(61));
        assertTrue(dedup.isFirstSeen("k1"));
    }

    @Test
    void testSweeperRemovesKeys() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertTrue(dedup.isFirstSeen("k2"));
        
        clock.setInstant(Instant.ofEpochSecond(1005L));
        assertTrue(dedup.isFirstSeen("k3"));
        
        assertFalse(dedup.isFirstSeen("k1"));
        assertFalse(dedup.isFirstSeen("k2"));
        assertFalse(dedup.isFirstSeen("k3"));
        
        clock.setInstant(Instant.ofEpochSecond(1070L));
        dedup.sweeper();
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertTrue(dedup.isFirstSeen("k2"));
        assertTrue(dedup.isFirstSeen("k3"));
    }

    @Test
    void testSweeperCleansUpExpiredBuckets() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(100L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(110L));
        assertTrue(dedup.isFirstSeen("k2"));
        
        clock.setInstant(Instant.ofEpochSecond(120L));
        assertTrue(dedup.isFirstSeen("k3"));
        
        assertFalse(dedup.isFirstSeen("k1"));
        assertFalse(dedup.isFirstSeen("k2"));
        assertFalse(dedup.isFirstSeen("k3"));
        
        clock.setInstant(Instant.ofEpochSecond(200L));
        dedup.sweeper();
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertTrue(dedup.isFirstSeen("k2"));
        assertTrue(dedup.isFirstSeen("k3"));
    }

    @Test
    void testConcurrentIsFirstSeenSameKey() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        int threadCount = 10;
        AtomicInteger firstSeenCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    if (dedup.isFirstSeen("k1")) {
                        firstSeenCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        startLatch.countDown();
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(1, firstSeenCount.get(), "Only one thread should see the key as first seen");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testConcurrentIsFirstSeenDifferentKeys() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        int threadCount = 100;
        Set<String> firstSeenKeys = ConcurrentHashMap.newKeySet();
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            final String key = "k" + i;
            executor.submit(() -> {
                try {
                    if (dedup.isFirstSeen(key)) {
                        firstSeenKeys.add(key);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(threadCount, firstSeenKeys.size(), "All unique keys should be seen as first seen");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testConcurrentAccessWithExpiration() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Boolean> results = new CopyOnWriteArrayList<>();
        
        clock.setInstant(Instant.ofEpochSecond(1030L));
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    results.add(dedup.isFirstSeen("k1"));
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        long falseCount = results.stream().filter(b -> !b).count();
        assertEquals(threadCount, falseCount, "All threads should see key as not first seen within TTL");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testSweeperRemovesExpiredKeysCorrectly() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertEquals(1, dedup.cacheSize());
        
        clock.setInstant(Instant.ofEpochSecond(1060L));
        dedup.sweeper();
        
        assertEquals(0, dedup.cacheSize(), "Sweeper should remove k1 from cache");
        assertTrue(dedup.isFirstSeen("k1"), "k1 should be seen as first after sweeper removal");
    }

    @Test
    void testKeyMovesToNewBucketAfterExpiration() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertFalse(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1061L));
        assertTrue(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1070L));
        assertFalse(dedup.isFirstSeen("k1"));
    }

    @Test
    void testMultipleKeysInSameBucket() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertTrue(dedup.isFirstSeen("k2"));
        assertTrue(dedup.isFirstSeen("k3"));
        
        assertFalse(dedup.isFirstSeen("k1"));
        assertFalse(dedup.isFirstSeen("k2"));
        assertFalse(dedup.isFirstSeen("k3"));
        
        clock.setInstant(Instant.ofEpochSecond(1100L));
        dedup.sweeper();
        
        assertTrue(dedup.isFirstSeen("k1"));
        assertTrue(dedup.isFirstSeen("k2"));
        assertTrue(dedup.isFirstSeen("k3"));
    }

    @Test
    void testSweeperIncremental() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        assertTrue(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1005L));
        dedup.sweeper();
        assertFalse(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1010L));
        dedup.sweeper();
        assertFalse(dedup.isFirstSeen("k1"));
        
        clock.setInstant(Instant.ofEpochSecond(1061L));
        dedup.sweeper();
        assertTrue(dedup.isFirstSeen("k1"));
    }

    @Test
    void testConcurrentCorrectnessWithOverlappingKeys() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        int threadCount = 50;
        int keyCount = 100;
        Map<String, AtomicInteger> trueCountPerKey = new ConcurrentHashMap<>();
        for (int i = 0; i < keyCount; i++) {
            trueCountPerKey.put("k" + i, new AtomicInteger(0));
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < keyCount; i++) {
                        String key = "k" + i;
                        if (dedup.isFirstSeen(key)) {
                            trueCountPerKey.get(key).incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        for (int i = 0; i < keyCount; i++) {
            String key = "k" + i;
            assertEquals(1, trueCountPerKey.get(key).get(), 
                "Key " + key + " should return true exactly once across all threads");
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testConcurrentCorrectnessWithExpiration() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        int threadCount = 20;
        String key = "shared";
        AtomicInteger trueCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch firstRound = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    if (dedup.isFirstSeen(key)) {
                        trueCount.incrementAndGet();
                    }
                } finally {
                    firstRound.countDown();
                }
            });
        }
        
        assertTrue(firstRound.await(5, TimeUnit.SECONDS));
        assertEquals(1, trueCount.get(), "First round: exactly one thread should see key as first");
        
        clock.setInstant(Instant.ofEpochSecond(1061L));
        trueCount.set(0);
        CountDownLatch secondRound = new CountDownLatch(threadCount);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    if (dedup.isFirstSeen(key)) {
                        trueCount.incrementAndGet();
                    }
                } finally {
                    secondRound.countDown();
                }
            });
        }
        
        assertTrue(secondRound.await(5, TimeUnit.SECONDS));
        assertEquals(1, trueCount.get(), "Second round: exactly one thread should see expired key as first");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }

    @Test
    void testNoExceptionsUnderHighLoad() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        ConcurrentDeduplicator<String> dedup = new ConcurrentDeduplicator<>(TTL, clock);
        
        int threadCount = 50;
        int operationsPerThread = 1000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int t = 0; t < threadCount; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        dedup.isFirstSeen("k" + (i % 100));
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS));
        assertEquals(0, errors.get(), "No exceptions should occur under high load");
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
    }
}
