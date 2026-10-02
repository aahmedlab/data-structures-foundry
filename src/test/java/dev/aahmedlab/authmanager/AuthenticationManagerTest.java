package dev.aahmedlab.authmanager;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AuthenticationManagerTest {

  private static final int TTL = 5;
  private AuthenticationManager manager;

  @BeforeEach
  void setUp() {
    manager = new AuthenticationManager(TTL);
  }

  @Test
  void testInitialState() {
    assertEquals(0, manager.countUnexpiredTokens(1));
  }

  @Test
  void testGenerateSingleToken() {
    manager.generate("a", 1);
    assertEquals(1, manager.countUnexpiredTokens(1));
  }

  @Test
  void testGenerateMultipleTokens() {
    manager.generate("a", 1);
    manager.generate("b", 2);
    manager.generate("c", 3);
    assertEquals(3, manager.countUnexpiredTokens(3));
  }

  @Test
  void testRegenerateActiveTokenCountsOnceAndResetsExpiry() {
    manager.generate("a", 1); // expires at 6
    manager.generate("a", 4); // expires at 9
    assertEquals(1, manager.countUnexpiredTokens(4));
    assertEquals(1, manager.countUnexpiredTokens(8));
    assertEquals(0, manager.countUnexpiredTokens(9));
  }

  @Test
  void testRegenerateExpiredTokenBeforeCleanupCountsOnce() {
    manager.generate("a", 1); // expires at 6, never cleaned up
    manager.generate("a", 7); // expires at 12
    assertEquals(1, manager.countUnexpiredTokens(7));
    assertEquals(0, manager.countUnexpiredTokens(12));
  }

  @Test
  void testRegenerateExpiredTokenAfterCleanupCountsOnce() {
    manager.generate("a", 1); // expires at 6
    assertEquals(0, manager.countUnexpiredTokens(6));
    manager.generate("a", 7); // expires at 12
    assertEquals(1, manager.countUnexpiredTokens(7));
    assertEquals(0, manager.countUnexpiredTokens(12));
  }

  @ParameterizedTest
  @CsvSource({
    "1, 1", // same instant
    "5, 1", // one before expiry
    "6, 0", // exactly at expiry: expired
    "7, 0", // after expiry
  })
  void testExpiryBoundary(int queryTime, int expectedCount) {
    manager.generate("a", 1); // expires at 6
    assertEquals(expectedCount, manager.countUnexpiredTokens(queryTime));
  }

  @Test
  void testTokensExpireIndependently() {
    manager.generate("a", 1); // expires at 6
    manager.generate("b", 3); // expires at 8
    assertEquals(2, manager.countUnexpiredTokens(5));
    assertEquals(1, manager.countUnexpiredTokens(6));
    assertEquals(1, manager.countUnexpiredTokens(7));
    assertEquals(0, manager.countUnexpiredTokens(8));
  }

  @Test
  void testRenewExtendsExpiry() {
    manager.generate("a", 1); // expires at 6
    manager.renew("a", 4); // expires at 9
    assertEquals(1, manager.countUnexpiredTokens(6));
    assertEquals(1, manager.countUnexpiredTokens(8));
    assertEquals(0, manager.countUnexpiredTokens(9));
  }

  @Test
  void testRenewMultipleTimes() {
    manager.generate("a", 1); // expires at 6
    manager.renew("a", 2); // expires at 7
    manager.renew("a", 5); // expires at 10
    manager.renew("a", 9); // expires at 14
    assertEquals(1, manager.countUnexpiredTokens(13));
    assertEquals(0, manager.countUnexpiredTokens(14));
  }

  @Test
  void testRenewUnknownTokenIsNoOp() {
    manager.renew("missing", 1);
    assertEquals(0, manager.countUnexpiredTokens(1));

    manager.generate("a", 1);
    manager.renew("missing", 2);
    assertEquals(1, manager.countUnexpiredTokens(2));
  }

  @Test
  void testRenewAtExactExpiryIsIgnored() {
    manager.generate("a", 1); // expires at 6
    manager.renew("a", 6);
    assertEquals(0, manager.countUnexpiredTokens(6));
    assertEquals(0, manager.countUnexpiredTokens(7));
  }

  @Test
  void testRenewAfterExpiryIsIgnored() {
    manager.generate("a", 1); // expires at 6
    manager.renew("a", 8);
    assertEquals(0, manager.countUnexpiredTokens(8));
    assertEquals(0, manager.countUnexpiredTokens(10));
  }

  @Test
  void testRenewExpiredTokenAfterCleanupIsIgnored() {
    manager.generate("a", 1); // expires at 6
    assertEquals(0, manager.countUnexpiredTokens(7));
    manager.renew("a", 8);
    assertEquals(0, manager.countUnexpiredTokens(8));
  }

  @Test
  void testRenewExpiredTokenDoesNotAffectOthers() {
    manager.generate("a", 1); // expires at 6
    manager.generate("b", 4); // expires at 9
    manager.renew("a", 7); // ignored, a already expired
    assertEquals(1, manager.countUnexpiredTokens(7));
    assertEquals(0, manager.countUnexpiredTokens(9));
  }

  @Test
  void testStaleHeapEntryDoesNotExpireRenewedToken() {
    manager.generate("a", 1); // expires at 6
    manager.generate("b", 2); // expires at 7
    manager.renew("a", 5); // expires at 10; stale heap entry at 6 remains
    assertEquals(2, manager.countUnexpiredTokens(6));
    assertEquals(1, manager.countUnexpiredTokens(7));
    assertEquals(1, manager.countUnexpiredTokens(9));
    assertEquals(0, manager.countUnexpiredTokens(10));
  }

  @Test
  void testRepeatedCountsAreStable() {
    manager.generate("a", 1);
    manager.generate("b", 2);
    assertEquals(2, manager.countUnexpiredTokens(3));
    assertEquals(2, manager.countUnexpiredTokens(3));
    assertEquals(1, manager.countUnexpiredTokens(6));
    assertEquals(1, manager.countUnexpiredTokens(6));
  }

  @Test
  void testCountNeverNegativeAfterMixedOperations() {
    manager.generate("a", 1); // expires at 6
    manager.renew("a", 7); // expired, removed
    manager.renew("a", 8); // unknown now, no-op
    assertEquals(0, manager.countUnexpiredTokens(20));
  }

  @Test
  void testLeetCodeExample() {
    AuthenticationManager m = new AuthenticationManager(5);
    m.renew("aaa", 1);
    m.generate("aaa", 2);
    assertEquals(1, m.countUnexpiredTokens(6));
    m.generate("bbb", 7);
    m.renew("aaa", 8);
    m.renew("bbb", 10);
    assertEquals(0, m.countUnexpiredTokens(15));
  }

  @Test
  void testTtlOfOneExpiresOnNextTick() {
    AuthenticationManager m = new AuthenticationManager(1);
    m.generate("a", 10);
    assertEquals(1, m.countUnexpiredTokens(10));
    assertEquals(0, m.countUnexpiredTokens(11));
  }

  @Test
  void testManyTokensSameTime() {
    for (int i = 0; i < 100; i++) {
      manager.generate("t" + i, 1); // all expire at 6
    }
    assertEquals(100, manager.countUnexpiredTokens(5));
    assertEquals(0, manager.countUnexpiredTokens(6));
  }

  @Test
  void testStaggeredTokensExpireInOrder() {
    for (int t = 1; t <= 10; t++) {
      manager.generate("t" + t, t); // expires at t + TTL
    }
    assertEquals(5, manager.countUnexpiredTokens(10)); // t6..t10
    assertEquals(3, manager.countUnexpiredTokens(12)); // t8..t10
    assertEquals(0, manager.countUnexpiredTokens(15));
  }
}
