package dev.aahmedlab.logging;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SimpleFormatterTest {

  private final SimpleFormatter formatter = new SimpleFormatter();

  @Test
  void testFormatsLevelInBracketsFollowedByMessage() {
    assertEquals("[ERROR] boom", formatter.format(new LogMessage(40, "ERROR", "boom")));
  }

  @Test
  void testIgnoresNumericLevelKey() {
    assertEquals("[INFO] x", formatter.format(new LogMessage(999, "INFO", "x")));
  }

  @Test
  void testEmptyMessage() {
    assertEquals("[WARN] ", formatter.format(new LogMessage(30, "WARN", "")));
  }

  @Test
  void testMessageIsNotTreatedAsFormatPattern() {
    assertEquals(
        "[INFO] 100% done %s", formatter.format(new LogMessage(20, "INFO", "100% done %s")));
  }
}
