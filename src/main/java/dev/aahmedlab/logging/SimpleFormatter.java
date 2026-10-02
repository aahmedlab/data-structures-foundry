package dev.aahmedlab.logging;

public class SimpleFormatter implements Formatter {
  public String format(LogMessage logMessage) {
    return String.format("[%s] %s", logMessage.getLevel(), logMessage.getMessage());
  }
}
