package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Resolves the conflicts of one timetable cell by relocating each conflicted
 * dual session to its first conflict-free session time (teacher free and
 * unblocked in both periods, weekly hours within the limit, every student of
 * the subject free in both periods). Classes with no conflict-free placement
 * anywhere are left in place and reported.
 */
public final class ConflictSolver {

  /**
   * Tries to move every conflicted session covering (day, period) to a
   * conflict-free dual session. Sessions are counted once even though their
   * two periods appear as separate slots; each move is applied to the database
   * immediately and the caches are refreshed, so later moves account for
   * earlier ones. Returns the number of sessions moved; sessions still
   * conflicted afterwards had no free placement.
   */
  public static int solveCell(int day, int period) throws SQLException {
    int moved = 0;
    // Snapshot first: one session per id (both of its periods share the id).
    List<Slot> cell = distinctSessions(Views.slots.stream()
        .filter(s -> s.day() == day && TimetableDao.sessionStart(s.period()) == TimetableDao.sessionStart(period))
        .toList());
    for (Slot slot : cell) {
      Views.refreshData();
      if (isConflictFree(slot.id())) {
        continue; // an earlier move may already have resolved this class
      }
      List<int[]> free = TimeSuggester.findFreeSessions(slot.subject(), slot.teacher(),
          Views.slots, Views.students, Views.teachers, slot.id());
      if (free.isEmpty()) {
        continue; // nothing can fix this class; the caller reports it
      }
      TimetableDao.move(slot.id(), free.get(0)[0], free.get(0)[1]);
      moved++;
    }
    Views.refreshData();
    return moved;
  }

  /** Both periods of a session share the id, so dedupe by id in encounter order. */
  private static List<Slot> distinctSessions(List<Slot> expanded) {
    List<Slot> unique = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    for (Slot slot : expanded) {
      if (seen.add(slot.id())) {
        unique.add(slot);
      }
    }
    return unique;
  }

  private static boolean isConflictFree(int slotId) {
    List<String> reasons = Views.conflicts.get(slotId);
    return reasons == null || reasons.isEmpty();
  }

  /** Sessions of this cell's classes that remain conflicted (for reporting). */
  public static List<Slot> stillConflicted(int day, int period) {
    return distinctSessions(Views.slots.stream()
        .filter(s -> s.day() == day && TimetableDao.sessionStart(s.period()) == TimetableDao.sessionStart(period))
        .filter(s -> !isConflictFree(s.id()))
        .toList());
  }

  private ConflictSolver() {
  }
}
