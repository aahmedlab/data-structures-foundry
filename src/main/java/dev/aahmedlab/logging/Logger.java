package dev.aahmedlab.logging;

import java.util.*;

public class Logger {
  static final Map<String, Integer> logLevel =
      Map.of("DEBUG", 10, "INFO", 20, "WARN", 30, "ERROR", 40);
  private int minLogLevel;
  private Set<Appender> registeredAppenders = new HashSet<>();

  public Logger() {
    minLogLevel = logLevel.get("INFO");
    registeredAppenders.add(new MemoryAppender(new SimpleFormatter()));
  }

  public void addAppender(final Appender appender) {
    registeredAppenders.add(appender);
  }

  public void log(final String level, final String message) {
    if (logLevel.get(level) < minLogLevel) return;
    LogMessage logMessage = new LogMessage(logLevel.get(level), level, message);
    for (Appender appender : registeredAppenders) {
      appender.append(logMessage);
    }
  }

  public void setLevel(final String level) {
    minLogLevel = logLevel.get(level);
  }
}
