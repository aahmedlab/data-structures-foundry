package dev.aahmedlab.logging;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoggerTest {

  private Logger logger;
  private List<LogMessage> received;

  @BeforeEach
  void setUp() {
    logger = new Logger();
    received = new ArrayList<>();
    logger.addAppender(received::add);
  }

  private List<String> receivedLevels() {
    return received.stream().map(LogMessage::getLevel).toList();
  }

  @Test
  void testDefaultLevelIsInfo() {
    logger.log("DEBUG", "d");
    logger.log("INFO", "i");
    logger.log("WARN", "w");
    logger.log("ERROR", "e");

    assertEquals(List.of("INFO", "WARN", "ERROR"), receivedLevels());
  }

  @Test
  void testMessageAtExactlyMinimumLevelIsLogged() {
    logger.setLevel("WARN");
    logger.log("WARN", "boundary");

    assertEquals(1, received.size());
  }

  @ParameterizedTest(name = "min={0} -> {1}")
  @CsvSource({
    "DEBUG, DEBUG INFO WARN ERROR",
    "INFO,  INFO WARN ERROR",
    "WARN,  WARN ERROR",
    "ERROR, ERROR",
  })
  void testSetLevelFiltersLowerLevels(String minLevel, String expectedLevels) {
    logger.setLevel(minLevel);
    for (String level : List.of("DEBUG", "INFO", "WARN", "ERROR")) {
      logger.log(level, "msg");
    }

    assertEquals(List.of(expectedLevels.split(" ")), receivedLevels());
  }

  @Test
  void testSetLevelCanLowerThreshold() {
    logger.setLevel("ERROR");
    logger.log("INFO", "dropped");
    logger.setLevel("DEBUG");
    logger.log("DEBUG", "kept");

    assertEquals(List.of("DEBUG"), receivedLevels());
  }

  @Test
  void testLogMessageCarriesLevelRankAndText() {
    logger.log("WARN", "disk almost full");

    LogMessage msg = received.get(0);
    assertEquals("WARN", msg.getLevel());
    assertEquals(30, msg.getLevelKey());
    assertEquals("disk almost full", msg.getMessage());
  }

  @Test
  void testLevelRanksAreOrdered() {
    assertTrue(Logger.logLevel.get("DEBUG") < Logger.logLevel.get("INFO"));
    assertTrue(Logger.logLevel.get("INFO") < Logger.logLevel.get("WARN"));
    assertTrue(Logger.logLevel.get("WARN") < Logger.logLevel.get("ERROR"));
  }

  @Test
  void testMessagesArriveInLoggingOrder() {
    logger.log("INFO", "first");
    logger.log("ERROR", "second");
    logger.log("WARN", "third");

    assertEquals(
        List.of("first", "second", "third"),
        received.stream().map(LogMessage::getMessage).toList());
  }

  @Test
  void testEveryRegisteredAppenderReceivesEachMessage() {
    List<LogMessage> second = new ArrayList<>();
    logger.addAppender(second::add);

    logger.log("INFO", "fan out");

    assertEquals(1, received.size());
    assertEquals(1, second.size());
    assertSame(received.get(0), second.get(0), "All appenders should get the same message");
  }

  @Test
  void testFilteredMessageReachesNoAppender() {
    List<LogMessage> second = new ArrayList<>();
    logger.addAppender(second::add);

    logger.log("DEBUG", "below INFO");

    assertTrue(received.isEmpty());
    assertTrue(second.isEmpty());
  }

  @Test
  void testAddingSameAppenderTwiceDeliversOnce() {
    MemoryAppender appender = new MemoryAppender(new SimpleFormatter());
    logger.addAppender(appender);
    logger.addAppender(appender);

    logger.log("INFO", "once");

    assertEquals(List.of("[INFO] once"), appender.getLogMessages());
  }

  @Test
  void testWorksWithMemoryAppenderAndSimpleFormatter() {
    MemoryAppender appender = new MemoryAppender(new SimpleFormatter());
    logger.addAppender(appender);
    logger.setLevel("WARN");

    logger.log("INFO", "ignored");
    logger.log("WARN", "low disk");
    logger.log("ERROR", "disk full");

    assertEquals(List.of("[WARN] low disk", "[ERROR] disk full"), appender.getLogMessages());
  }
}
