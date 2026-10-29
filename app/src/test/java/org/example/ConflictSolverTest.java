package org.example;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end conflict solving through the embedded database (Solver% rows only). */
class ConflictSolverTest {

  private static final int DAY = 2;

  @BeforeEach
  void cleanTestRows() throws Exception {
    Database.start();
    cleanup();
  }

  @AfterEach
  void cleanUpAfter() throws Exception {
    cleanup();
  }

  private static void cleanup() throws Exception {
    try (Connection c = Database.open(); Statement s = c.createStatement()) {
      s.executeUpdate("DELETE FROM teacher_unavailability WHERE teacher_id IN "
          + "(SELECT id FROM teachers WHERE name LIKE 'Solver%')");
      s.executeUpdate("DELETE FROM timetable_slots WHERE teacher_id IN "
          + "(SELECT id FROM teachers WHERE name LIKE 'Solver%')");
      s.executeUpdate("DELETE FROM teachers WHERE name LIKE 'Solver%'");
      s.executeUpdate("DELETE FROM students WHERE name LIKE 'Solver%'");
    }
  }

  private static Slot save(int period, String subject, String teacher) throws Exception {
    return TimetableDao.save(DAY, period,
        new SchoolClass(new Subject(subject), new Teacher(teacher)));
  }

  @Test
  void movingOneClashingClassResolvesBoth() throws Exception {
    List<String> subjects = SubjectDao.all().stream().map(Subject::getName).toList();
    assertTrue(subjects.size() >= 2);
    String subjectA = subjects.get(0);
    String subjectB = subjects.get(1);
    TeacherDao.findOrCreate("Solver Alpha");
    TeacherDao.findOrCreate("Solver Beta");
    Slot slotA = save(0, subjectA, "Solver Alpha");
    Slot slotB = save(0, subjectB, "Solver Beta");
    Student student = new Student("Solver Ana", "Grade 11");
    StudentDao.upsert(student, List.of(
        SubjectDao.findOrCreate(subjectA), SubjectDao.findOrCreate(subjectB)));

    Views.refreshData();
    List<String> reasons = Views.conflicts.get(slotB.id());
    assertTrue(reasons != null && !reasons.isEmpty(), "setup should create a student clash");

    int moved = ConflictSolver.solveCell(DAY, 0);

    assertEquals(1, moved);
    Views.refreshData();
    assertTrue(Views.conflicts.get(slotA.id()) == null
        || Views.conflicts.get(slotA.id()).isEmpty(), "clash should be gone");
    // The clashing classes no longer share a time cell.
    List<Slot> after = TimetableDao.slots(null);
    Slot afterA = after.stream().filter(s -> s.id() == slotA.id()).findFirst().orElseThrow();
    Slot afterB = after.stream().filter(s -> s.id() == slotB.id()).findFirst().orElseThrow();
    assertTrue(afterA.day() != afterB.day() || afterA.period() != afterB.period(),
        "the two classes should sit in different cells now");
  }

  @Test
  void classWithoutAnyFreePlacementStaysConflicted() throws Exception {
    String subject = SubjectDao.all().stream().map(Subject::getName).findFirst().orElseThrow();
    Teacher gamma = TeacherDao.findOrCreate("Solver Gamma");
    gamma.setMaxHoursPerWeek(1);
    TeacherDao.upsert(gamma, List.of(SubjectDao.findOrCreate(subject)), List.of());
    Slot first = save(0, subject, "Solver Gamma");
    save(1, subject, "Solver Gamma"); // exceeds max hours of 1

    Views.refreshData();
    List<String> reasons = Views.conflicts.get(first.id());
    assertTrue(reasons != null && !reasons.isEmpty());

    int moved = ConflictSolver.solveCell(DAY, 0);

    assertEquals(0, moved, "a teacher already over max hours cannot stop conflicting");
    List<Slot> unresolved = ConflictSolver.stillConflicted(DAY, 0);
    assertEquals(1, unresolved.size());
    assertTrue(unresolved.get(0).id() == first.id());
  }
}
