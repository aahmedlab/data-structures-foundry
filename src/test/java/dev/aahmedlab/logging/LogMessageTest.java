package dev.aahmedlab.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LogMessageTest {

  @Test
  void testGettersReturnConstructorValues() {
    LogMessage msg = new LogMessage(30, "WARN", "careful");

    assertEquals(30, msg.getLevelKey());
    assertEquals("WARN", msg.getLevel());
    assertEquals("careful", msg.getMessage());
  }
}
