package dev.aahmedlab.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemoryAppenderTest {

  private MemoryAppender appender;

  @BeforeEach
  void setUp() {
    appender = new MemoryAppender(new SimpleFormatter());
  }

  @Test
  void testStartsEmpty() {
    assertTrue(appender.getLogMessages().isEmpty());
  }

  @Test
  void testStoresFormattedMessagesInOrder() {
    appender.append(new LogMessage(20, "INFO", "first"));
    appender.append(new LogMessage(40, "ERROR", "second"));

    assertEquals(List.of("[INFO] first", "[ERROR] second"), appender.getLogMessages());
  }

  @Test
  void testUsesTheGivenFormatter() {
    MemoryAppender custom = new MemoryAppender(m -> m.getLevelKey() + ":" + m.getMessage());
    custom.append(new LogMessage(30, "WARN", "hot"));

    assertEquals(List.of("30:hot"), custom.getLogMessages());
  }

  @Test
  void testGetLogMessagesReturnsDefensiveCopy() {
    appender.append(new LogMessage(20, "INFO", "kept"));

    List<String> snapshot = appender.getLogMessages();
    snapshot.clear();
    snapshot.add("injected");

    assertEquals(List.of("[INFO] kept"), appender.getLogMessages());
  }

  @Test
  void testSnapshotDoesNotSeeLaterAppends() {
    appender.append(new LogMessage(20, "INFO", "one"));
    List<String> snapshot = appender.getLogMessages();

    appender.append(new LogMessage(20, "INFO", "two"));

    assertEquals(List.of("[INFO] one"), snapshot);
  }

  @Test
  void testCloseClearsMessages() {
    appender.append(new LogMessage(20, "INFO", "gone"));
    appender.close();

    assertTrue(appender.getLogMessages().isEmpty());
  }

  @Test
  void testAppendStillWorksAfterClose() {
    appender.append(new LogMessage(20, "INFO", "before"));
    appender.close();
    appender.append(new LogMessage(20, "INFO", "after"));

    assertEquals(List.of("[INFO] after"), appender.getLogMessages());
  }
}
