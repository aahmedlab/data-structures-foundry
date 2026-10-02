package dev.aahmedlab.logging;

public class LogMessage {
  private final int levelKey;
  private final String level;
  private final String message;

  public LogMessage(final int levelKey, final String level, final String message) {
    this.levelKey = levelKey;
    this.level = level;
    this.message = message;
  }

  public int getLevelKey() {
    return levelKey;
  }

  public String getLevel() {
    return level;
  }

  public String getMessage() {
    return message;
  }
}
