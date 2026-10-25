package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Conflict rules per criterion: student clashes, teacher double-booking, no-time slots. */
class ConflictDetectorTest {

  private static Slot slot(int id, int day, int period, String subject, String teacher) {
    return new Slot(id, day, period, subject, teacher, "", "");
  }

  @Test
  void studentClashWhenTwoTakenSubjectsShareSlot() {
    Slot spanish = slot(1, 0, 0, "Spanish", "T1");
    Slot biology = slot(2, 0, 0, "Biology", "T2");
    Student student = new Student("Ana", "Grade 11");
    student.addCourse(new Subject("Spanish"));
    student.addCourse(new Subject("Biology"));
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        List.of(spanish, biology), List.of(student), List.of());
    assertTrue(conflicts.get(1).contains("Ana has two classes (Spanish + Biology) at this time"));
    assertTrue(conflicts.get(2).contains("Ana has two classes (Spanish + Biology) at this time"));
  }

  @Test
  void noClashWhenSubjectsAtDifferentTimes() {
    Slot spanish = slot(1, 0, 0, "Spanish", "T1");
    Slot biology = slot(2, 1, 1, "Biology", "T2");
    Student student = new Student("Ana", "Grade 11");
    student.addCourse(new Subject("Spanish"));
    student.addCourse(new Subject("Biology"));
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        List.of(spanish, biology), List.of(student), List.of());
    assertTrue(conflicts.isEmpty());
  }

  @Test
  void teacherDoubleBookingDetected() {
    Slot a = slot(1, 2, 3, "Spanish", "T1");
    Slot b = slot(2, 2, 3, "Biology", "T1");
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        List.of(a, b), List.of(), List.of());
    assertTrue(conflicts.get(1).stream().anyMatch(r -> r.contains("booked twice")));
    assertTrue(conflicts.get(2).stream().anyMatch(r -> r.contains("booked twice")));
  }

  @Test
  void teacherBlockOccupiedDetected() {
    Teacher teacher = new Teacher("Nata");
    teacher.setId(5);
    teacher.installBlocks(List.of(new int[] { 1, 2 }));
    Slot occupied = slot(1, 1, 2, "Spanish", "Nata");
    Slot free = slot(2, 1, 1, "Spanish", "Nata");
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        List.of(occupied, free), List.of(), List.of(teacher));
    assertTrue(conflicts.get(1).stream().anyMatch(r -> r.contains("no-time slot")));
    assertTrue(!conflicts.containsKey(2));
  }

  @Test
  void teacherOverMaxHoursDetected() {
    Teacher teacher = new Teacher("Nata");
    Slot first = slot(1, 0, 0, "Spanish", "Nata");
    Slot second = slot(2, 0, 1, "Spanish", "Nata");
    Slot third = slot(3, 0, 2, "Spanish", "Nata");
    teacher.setMaxHoursPerWeek(2);
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        List.of(first, second, third), List.of(), List.of(teacher));
    assertTrue(conflicts.get(1).stream().anyMatch(r -> r.contains("over the limit")));
    assertTrue(conflicts.get(3).stream().anyMatch(r -> r.contains("over the limit")));
  }
}
