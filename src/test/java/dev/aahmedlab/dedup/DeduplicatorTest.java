package dev.aahmedlab.dedup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeduplicatorTest {

    private static final int TTL = 60;
    private Deduplicator<String> deduplicator;

    @BeforeEach
    void setUp() {
        deduplicator = new Deduplicator<>(TTL);
    }

    @Test
    void testSingleHit() {
        assertTrue(deduplicator.isFirstSeen("k1"));
    }

    @Test
    void testSameKeyWithinTheTTLTime() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        Deduplicator<String> testDedup = new Deduplicator<>(TTL, clock);

        assertTrue(testDedup.isFirstSeen("k1"));

        clock.setInstant(Instant.ofEpochSecond(1010L));
        assertFalse(testDedup.isFirstSeen("k1"));
    }

    @Test
    void testKeyExpiresAfter60Seconds() {
        Instant startTime = Instant.ofEpochSecond(1000L);
        MutableClock clock = new MutableClock(startTime, ZoneId.of("UTC"));
        Deduplicator<String> dedup = new Deduplicator<>(TTL, clock);

        assertTrue(dedup.isFirstSeen("k1"));

        clock.setInstant(startTime.plusSeconds(60));
        assertFalse(dedup.isFirstSeen("k1"));

        clock.setInstant(startTime.plusSeconds(61));
        assertTrue(dedup.isFirstSeen("k1"));
    }

    @Test
    void testSweeperRemovesKeys() {
        MutableClock clock = new MutableClock(Instant.ofEpochSecond(1000L), ZoneId.of("UTC"));
        Deduplicator<String> dedup = new Deduplicator<>(TTL, clock);

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
        Deduplicator<String> dedup = new Deduplicator<>(TTL, clock);

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

    static class MutableClock extends Clock {
        private final ZoneId zone;
        private Instant currentInstant;

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
}
