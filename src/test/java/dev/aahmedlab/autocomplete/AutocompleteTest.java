package dev.aahmedlab.autocomplete;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AutocompleteTest {

  @Test
  void testConstructsWithValidK() {
    assertDoesNotThrow(() -> new Autocomplete(5));
  }

  @Test
  void testRejectsNonPositiveK() {
    assertThrows(IllegalArgumentException.class, () -> new Autocomplete(0));
    assertThrows(IllegalArgumentException.class, () -> new Autocomplete(-3));
  }

  @Test
  void testEntryRecordExposesComponents() {
    Entry entry = new Entry("hello", 7);
    assertEquals("hello", entry.word());
    assertEquals(7, entry.freq());
  }
}
