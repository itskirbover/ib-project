package org.example;

import java.sql.SQLException;
import java.util.List;

/**
 * Resolves the conflicts of one timetable cell by relocating each conflicted
 * class to its first conflict-free time (teacher free and unblocked, weekly
 * hours within the limit, every student of the subject free). Classes with no
 * conflict-free placement anywhere are left in place and reported.
 */
public final class ConflictSolver {

  /**
   * Tries to move every conflicted class at (day, period) to a conflict-free
   * slot. Each move is applied to the database immediately and the caches are
   * refreshed, so later moves account for earlier ones. Returns the number of
   * classes moved; slots still conflicted afterwards had no free placement.
   */
  public static int solveCell(int day, int period) throws SQLException {
    int moved = 0;
    // Snapshot first: moves must not disturb the loop while other classes may
    // still sit in this cell needing attention.
    List<Slot> cell = Views.slots.stream()
        .filter(s -> s.day() == day && s.period() == period)
        .toList();
    for (Slot slot : cell) {
      Views.refreshData();
      if (isConflictFree(slot.id())) {
        continue; // an earlier move may already have resolved this class
      }
      List<int[]> free = TimeSuggester.findFreeSlots(slot.subject(), slot.teacher(),
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

  private static boolean isConflictFree(int slotId) {
    List<String> reasons = Views.conflicts.get(slotId);
    return reasons == null || reasons.isEmpty();
  }

  /** Subjects of this cell's classes that remain conflicted (for reporting). */
  public static List<Slot> stillConflicted(int day, int period) {
    return Views.slots.stream()
        .filter(s -> s.day() == day && s.period() == period)
        .filter(s -> !isConflictFree(s.id()))
        .toList();
  }

  private ConflictSolver() {
  }
}
