package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Suggests conflict-free (day, period) placements for a proposed class of one
 * subject taught by one teacher: the teacher may not already teach or be
 * blocked at that time (nor exceed their weekly hours), and every student of
 * the subject must be free at that time as well.
 */
public final class TimeSuggester {

  /**
   * All conflict-free placements sorted so the first is {0, 0}. When
   * {@code excludeSlotId} is positive, that existing slot (a class being
   * moved) is left out of the analysis so its own time does not block itself.
   */
  public static List<int[]> findFreeSlots(String subjectName, String teacherName,
      List<Slot> slots, List<Student> students, List<Teacher> teachers, int excludeSlotId) {
    List<Slot> considered = excludeSlotId > 0
        ? slots.stream().filter(s -> s.id() != excludeSlotId).toList()
        : slots;
    Teacher teacher = teacherByName(teachers, teacherName);
    List<Student> roster = studentsOfSubject(students, subjectName);
    List<int[]> free = new ArrayList<>();
    for (int day = 0; day < Palette.DAYS; day++) {
      for (int period = 0; period < Palette.PERIODS; period++) {
        if (isFree(day, period, subjectName, teacher, roster, considered)) {
          free.add(new int[] { day, period });
        }
      }
    }
    return free;
  }

  /** True if placing the class at (day, period) would create no conflict. */
  static boolean isFree(int day, int period, String subjectName, Teacher teacher,
      List<Student> roster, List<Slot> slots) {
    for (Slot slot : slots) {
      if (slot.day() != day || slot.period() != period) {
        continue;
      }
      if (teacher != null && slot.teacher().equals(teacher.getName())) {
        return false; // teacher would be double-booked
      }
      for (Student student : roster) {
        boolean takesSlotSubject = student.getCourses().stream()
            .anyMatch(sub -> sub.getName().equals(slot.subject()));
        if (takesSlotSubject && !slot.subject().equals(subjectName)) {
          return false; // that student has another class at this time
        }
      }
    }
    if (teacher != null) {
      if (!teacher.isAvailable(day, period)) {
        return false; // teacher marked this slot as a no-time slot
      }
      long taught = slots.stream().filter(s -> s.teacher().equals(teacher.getName())).count();
      int max = teacher.getMaxHoursPerWeek();
      if (max > 0 && taught + 1 > max) {
        return false; // placing here would exceed the weekly limit
      }
    }
    return true;
  }

  private static List<Student> studentsOfSubject(List<Student> students, String subjectName) {
    return students.stream()
        .filter(s -> s.getCourses().stream().anyMatch(sub -> sub.getName().equals(subjectName)))
        .toList();
  }

  private static Teacher teacherByName(List<Teacher> teachers, String name) {
    return teachers.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
  }

  private TimeSuggester() {
  }
}
