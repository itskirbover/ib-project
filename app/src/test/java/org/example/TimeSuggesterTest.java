package org.example;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Conflict-free slot suggestion rules mirror ConflictDetector. */
class TimeSuggesterTest {

  private static Slot slot(int id, int day, int period, String subject, String teacher) {
    return new Slot(id, day, period, subject, teacher, "", "");
  }

  private static List<int[]> free(String subject, String teacher, List<Slot> slots,
      List<Student> students, List<Teacher> teachers) {
    return TimeSuggester.findFreeSlots(subject, teacher, slots, students, teachers, 0);
  }

  private static int at(List<int[]> slotsList, int day, int period) {
    for (int i = 0; i < slotsList.size(); i++) {
      int[] candidate = slotsList.get(i);
      if (candidate[0] == day && candidate[1] == period) {
        return i;
      }
    }
    return -1;
  }

  @Test
  void emptyTimetableOffersEverySlotInOrder() {
    List<int[]> candidates = free("Biology", "Nata", List.of(), List.of(), List.of());
    assertEquals(Palette.DAYS * Palette.PERIODS, candidates.size());
    assertEquals(0, candidates.get(0)[0]);
    assertEquals(0, candidates.get(0)[1]);
  }

  @Test
  void teacherDoubleBookingExcluded() {
    Slot busy = slot(1, 0, 0, "Spanish", "Nata");
    Teacher nata = new Teacher("Nata");
    List<int[]> candidates = free("Biology", "Nata", List.of(busy), List.of(), List.of(nata));
    assertTrue(at(candidates, 0, 0) < 0);
    assertTrue(at(candidates, 0, 1) >= 0);
  }

  @Test
  void otherTeachersClassesDoNotBlock() {
    Slot busy = slot(1, 0, 0, "Spanish", "Rex");
    Teacher nata = new Teacher("Nata");
    List<int[]> candidates = free("Biology", "Nata", List.of(busy), List.of(), List.of(nata));
    assertTrue(at(candidates, 0, 0) >= 0);
  }

  @Test
  void studentOfSubjectBlocksTheirBusyTime() {
    Slot spanish = slot(1, 2, 3, "Spanish", "Rex");
    Student student = new Student("Ana", "Grade 11");
    student.addCourse(new Subject("Spanish"));
    student.addCourse(new Subject("Biology"));
    Teacher nata = new Teacher("Nata");
    List<int[]> candidates = free("Biology", "Nata", List.of(spanish), List.of(student),
        List.of(nata));
    assertTrue(at(candidates, 2, 3) < 0);
    assertTrue(at(candidates, 2, 4) >= 0);
  }

  @Test
  void studentsWithoutTheSubjectDoNotBlock() {
    Slot spanish = slot(1, 2, 3, "Spanish", "Rex");
    Student student = new Student("Ana", "Grade 11");
    student.addCourse(new Subject("Spanish"));
    Teacher nata = new Teacher("Nata");
    List<int[]> candidates = free("Biology", "Nata", List.of(spanish), List.of(student),
        List.of(nata));
    assertTrue(at(candidates, 2, 3) >= 0);
  }

  @Test
  void blockedSlotExcluded() {
    Teacher teacher = new Teacher("Nata");
    teacher.installBlocks(List.of(new int[] { 1, 2 }));
    List<int[]> candidates = free("Biology", "Nata", List.of(), List.of(), List.of(teacher));
    assertTrue(at(candidates, 1, 2) < 0);
  }

  @Test
  void teacherOverMaxHoursExcluded() {
    Teacher teacher = new Teacher("Nata");
    teacher.setMaxHoursPerWeek(2);
    Slot first = slot(1, 0, 0, "Spanish", "Nata");
    Slot second = slot(2, 0, 1, "Spanish", "Nata");
    // One weekly class left: new placements are still possible.
    List<int[]> allowed = free("Biology", "Nata", List.of(first), List.of(), List.of(teacher));
    assertTrue(!allowed.isEmpty());
    // At the limit: no slot anywhere accepts another class.
    List<int[]> candidates = free("Biology", "Nata", List.of(first, second), List.of(),
        List.of(teacher));
    assertTrue(candidates.isEmpty());
  }

  @Test
  void movingOwnSlotDoesNotBlockItself() {
    Teacher teacher = new Teacher("Nata");
    teacher.setMaxHoursPerWeek(1);
    Slot own = slot(1, 0, 0, "Spanish", "Nata");
    // Excluding its own id: the same weekly count stays, so moving is allowed.
    List<int[]> candidates = TimeSuggester.findFreeSlots("Spanish", "Nata", List.of(own),
        List.of(), List.of(teacher), 1);
    assertTrue(at(candidates, 1, 0) >= 0);
    // Without excluding, the hourly limit blocks every slot.
    List<int[]> blocked = free("Spanish", "Nata", List.of(own), List.of(), List.of(teacher));
    assertTrue(blocked.isEmpty());
  }
}
