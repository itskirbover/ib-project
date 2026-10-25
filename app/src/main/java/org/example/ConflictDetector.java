package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Detects scheduling clashes across the timetable:
 * - a student takes two different subjects that share a (day, period);
 * - one teacher classes in two parallel slots at the same (day, period);
 * - a class sits in one of its teacher's no-time slots;
 * - a teacher exceeds their max hours per week.
 */
public final class ConflictDetector {

  /** Slot id -> list of conflict reasons; slots without entries are conflict-free. */
  public static Map<Integer, List<String>> detect(List<Slot> slots, List<Student> students,
      List<Teacher> teachers) {
    Map<Integer, List<String>> conflicts = new HashMap<>();

    // Index slots by time for the shared-slot rules.
    Map<String, List<Slot>> byTime = new HashMap<>();
    for (Slot slot : slots) {
      byTime.computeIfAbsent(slot.day() + ":" + slot.period(), x -> new ArrayList<>()).add(slot);
    }

    // Student clash: two different subjects of the same student share a time period.
    for (Student student : students) {
      for (List<Slot> parallel : byTime.values()) {
        List<Slot> involved = new ArrayList<>();
        for (Slot slot : parallel) {
          if (student.getCourses().stream().anyMatch(sub -> sub.getName().equals(slot.subject()))) {
            involved.add(slot);
          }
        }
        if (involved.size() > 1) {
          String subjects = involved.stream().map(Slot::subject).distinct().reduce((a, b) -> a + " + " + b).orElse("");
          String reason = student.getName() + " has two classes (" + subjects + ") at this time";
          for (Slot slot : involved) {
            reasonFor(conflicts, slot.id()).add(reason);
          }
        }
      }
    }

    // Teacher double-booking.
    Map<String, Map<String, List<Slot>>> byTeacherAndTime = new HashMap<>();
    for (Slot slot : slots) {
      byTeacherAndTime.computeIfAbsent(slot.teacher(), x -> new HashMap<>())
          .computeIfAbsent(slot.day() + ":" + slot.period(), x -> new ArrayList<>())
          .add(slot);
    }
    for (Map<String, List<Slot>> byTimePerTeacher : byTeacherAndTime.values()) {
      for (List<Slot> parallel : byTimePerTeacher.values()) {
        if (parallel.size() > 1) {
          String reason = "Teacher " + parallel.get(0).teacher() + " is booked twice at this time";
          for (Slot slot : parallel) {
            reasonFor(conflicts, slot.id()).add(reason);
          }
        }
      }
    }

    // Teacher no-time slots occupied + weekly hour limits.
    for (Teacher teacher : teachers) {
      List<int[]> blocks = teacher.getBlockedSlots();
      List<Slot> taught = slots.stream().filter(s -> s.teacher().equals(teacher.getName())).toList();
      int max = teacher.getMaxHoursPerWeek();
      if (max > 0 && taught.size() > max) {
        String reason = "Teacher " + teacher.getName() + " teaches " + taught.size()
            + " classes, over the limit of " + max;
        for (Slot slot : taught) {
          reasonFor(conflicts, slot.id()).add(reason);
        }
      }
      for (Slot slot : taught) {
        for (int[] block : blocks) {
          if (block[0] == slot.day() && block[1] == slot.period()) {
            reasonFor(conflicts, slot.id()).add(
                "Teacher " + teacher.getName() + " marked this slot as a no-time slot");
          }
        }
      }
    }

    return conflicts;
  }

  private static List<String> reasonFor(Map<Integer, List<String>> conflicts, int slotId) {
    return conflicts.computeIfAbsent(slotId, x -> new ArrayList<>());
  }

  private ConflictDetector() {
  }
}
