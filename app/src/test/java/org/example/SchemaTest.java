package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Legacy single-period timetable rows must convert into aligned dual sessions
 * on migration (everything is delivered as two consecutive periods).
 */
class SchemaTest {

  private static Schema.LegacyRow row(int day, int period, int subject, int teacher) {
    return new Schema.LegacyRow(day, period, subject, teacher, "Grade 11", "201");
  }

  @Test
  void consecutivePairMergesIntoOneSession() {
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(0, 0, 1, 1), row(0, 1, 1, 1)));
    assertEquals(1, sessions.size());
    assertEquals(0, sessions.get(0).period());
  }

  @Test
  void oddSingleSnapsIntoItsContainingPair() {
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(row(2, 3, 1, 1)));
    assertEquals(1, sessions.size());
    assertEquals(2, sessions.get(0).period());
    assertEquals(2, sessions.get(0).day());
  }

  @Test
  void gapBetweenPeriodsKeepsSessionsApart() {
    // periods 0 and 3 of one class: pair 0-1 and pair 2-3
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(0, 0, 1, 1), row(0, 3, 1, 1)));
    assertEquals(2, sessions.size());
    assertEquals(0, sessions.get(0).period());
    assertEquals(2, sessions.get(1).period());
  }

  @Test
  void differentSubjectsStayParallelSessions() {
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(1, 0, 1, 1), row(1, 1, 2, 2)));
    assertEquals(2, sessions.size());
    assertTrue(sessions.stream().allMatch(s -> s.period() == 0));
  }

  @Test
  void duplicatesAgainstTheSessionKeyAreDropped() {
    // same subject twice in one pair collapses to a single session
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(1, 0, 1, 1), row(1, 0, 1, 1), row(1, 1, 1, 1)));
    assertEquals(1, sessions.size());
    assertEquals(0, sessions.get(0).period());
  }

  @Test
  void tripleRunBecomesPairPlusSnappedSingle() {
    // periods 0,1,2: pair 0-1 plus a snapped leftover in pair 2-3
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(0, 0, 1, 1), row(0, 1, 1, 1), row(0, 2, 1, 1)));
    assertEquals(2, sessions.size());
    assertEquals(0, sessions.get(0).period());
    assertEquals(2, sessions.get(1).period());
  }

  @Test
  void differentDaysNeverMerge() {
    List<Schema.LegacyRow> sessions = Schema.pairLegacyRows(List.of(
        row(0, 0, 1, 1), row(1, 1, 1, 1)));
    assertEquals(2, sessions.size());
    assertEquals(0, sessions.get(0).day());
    assertEquals(1, sessions.get(1).day());
  }
}
