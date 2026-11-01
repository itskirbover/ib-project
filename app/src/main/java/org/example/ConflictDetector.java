package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Detects scheduling clashes across the timetable:
 * - a student takes two different subjects that share a (day, period);
 * - one teacher classes in two parallel slots at the same (day, period);
 * - a class sits in one of its teacher's no-time slots;
 * - a teacher exceeds their max hours per week (periods are counted, so a dual session counts as 2);
 * - a subject's weekly period count misses the DP rule (HL must hold 6 periods = 3 dual sessions,
 *   SL must hold 4 periods = 2 dual sessions).
 */
public final class ConflictDetector {

  /**
   * Slot id -> list of conflict reasons; slots without entries are conflict-free.
   * Convenience overload: every subject defaults to SL.
   */
  public static Map<Integer, List<String>> detect(List<Slot> slots, List<Student> students,
      List<Teacher> teachers) {
    return detect(slots, students, teachers, Map.of());
  }

  /**
   * Full detection. {@code subjectLevels} maps subject name -> "HL"/"SL"
   * (anything missing defaults to SL).
   */
  public static Map<Integer, List<String>> detect(List<Slot> slots, List<Student> students,
      List<Teacher> teachers, Map<String, String> subjectLevels) {
    Map<Integer, Set<String>> raw = new HashMap<>();

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
            reasonFor(raw, slot.id()).add(reason);
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
            reasonFor(raw, slot.id()).add(reason);
          }
        }
      }
    }

    // Teacher no-time slots occupied + weekly period limits (dual sessions count twice).
    for (Teacher teacher : teachers) {
      List<int[]> blocks = teacher.getBlockedSlots();
      List<Slot> taught = slots.stream().filter(s -> s.teacher().equals(teacher.getName())).toList();
      int max = teacher.getMaxHoursPerWeek();
      if (max > 0 && taught.size() > max) {
        String reason = "Teacher " + teacher.getName() + " is scheduled for " + taught.size()
            + " periods, over the limit of " + max;
        for (Slot slot : taught) {
          reasonFor(raw, slot.id()).add(reason);
        }
      }
      for (Slot slot : taught) {
        for (int[] block : blocks) {
          if (block[0] == slot.day() && block[1] == slot.period()) {
            reasonFor(raw, slot.id()).add(
                "Teacher " + teacher.getName() + " marked this slot as a no-time slot");
          }
        }
      }
    }

    // Weekly count rule: HL subjects need 3 dual sessions (6 periods), SL need 2 (4 periods).
    checkWeeklyCount(slots, subjectLevels, raw);

    Map<Integer, List<String>> conflicts = new HashMap<>();
    for (Map.Entry<Integer, Set<String>> e : raw.entrySet()) {
      conflicts.put(e.getKey(), new ArrayList<>(e.getValue()));
    }
    return conflicts;
  }

  private static void checkWeeklyCount(List<Slot> slots, Map<String, String> subjectLevels,
      Map<Integer, Set<String>> raw) {
    Map<String, List<Slot>> bySubject = new LinkedHashMap<>();
    for (Slot slot : slots) {
      bySubject.computeIfAbsent(slot.subject(), x -> new ArrayList<>()).add(slot);
    }
    for (Map.Entry<String, List<Slot>> entry : bySubject.entrySet()) {
      String subject = entry.getKey();
      Subject probe = new Subject(subject);
      probe.setLevel(subjectLevels.getOrDefault(subject, "SL"));
      int required = probe.requiredSessions();
      int held = (int) entry.getValue().stream().map(Slot::id).distinct().count();
      if (held == required) {
        continue;
      }
      String reason = subject + " has " + held + " of " + required
          + " sessions a week (" + probe.requiredPeriods() + " periods)";
      for (Slot slot : entry.getValue()) {
        reasonFor(raw, slot.id()).add(reason);
      }
    }
  }

  private static Set<String> reasonFor(Map<Integer, Set<String>> conflicts, int slotId) {
    return conflicts.computeIfAbsent(slotId, x -> new LinkedHashSet<>());
  }

  private ConflictDetector() {
  }
}
