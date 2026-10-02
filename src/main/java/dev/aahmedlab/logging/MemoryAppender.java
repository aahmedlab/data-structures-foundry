package dev.aahmedlab.logging;

import java.util.ArrayList;
import java.util.List;

public class MemoryAppender implements Appender {
  private final Formatter formatter;
  private final List<String> logMessages;

  public MemoryAppender(Formatter formatter) {
    this.formatter = formatter;
    this.logMessages = new ArrayList<>();
  }

  public void append(LogMessage logMessage) {
    logMessages.add(formatter.format(logMessage));
  }

  public List<String> getLogMessages() {
    return new ArrayList<>(logMessages);
  }

  public void close() {
    logMessages.clear();
  }
}
