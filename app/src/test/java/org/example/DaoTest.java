package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Round-trips entities through the embedded database. */
class DaoTest {

  @BeforeEach
  void cleanTestRows() throws Exception {
    Database.start();
    try (Connection c = Database.open(); Statement s = c.createStatement()) {
      s.executeUpdate("DELETE FROM teacher_unavailability WHERE teacher_id IN "
          + "(SELECT id FROM teachers WHERE name LIKE 'Dao%')");
      s.executeUpdate("DELETE FROM timetable_slots WHERE subject_id IN "
          + "(SELECT id FROM subjects WHERE name LIKE 'Dao%')");
      s.executeUpdate("DELETE FROM teachers WHERE name LIKE 'Dao%'");
      s.executeUpdate("DELETE FROM students WHERE name LIKE 'Dao%'");
      s.executeUpdate("DELETE FROM subjects WHERE name LIKE 'Dao%'");
    }
  }

  @Test
  void ibCatalogSeeded() throws Exception {
    List<Subject> all = SubjectDao.all();
    assertTrue(all.size() >= IBSubjects.ALL.size());
    assertTrue(all.stream().anyMatch(s -> s.getName().equals("Mathematics: Analysis and Approaches")));
    assertTrue(all.stream().anyMatch(s -> s.getName().equals("English A: Literature")));
  }

  @Test
  void subjectFindOrCreateIsIdempotent() throws Exception {
    Subject first = SubjectDao.findOrCreate("Dao Subject A");
    Subject second = SubjectDao.findOrCreate("Dao Subject A");
    assertEquals(first.getId(), second.getId());
  }

  @Test
  void teacherFindOrCreateAndBlocks() throws Exception {
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher A");
    BlocksDao.add(teacher.getId(), 0, 3);
    assertTrue(new Teacher("Dao Teacher A").getBlockedSlots().isEmpty()); // fresh instance lazy-loads per id
    Teacher loaded = TeacherDao.findOrCreate("Dao Teacher A");
    assertTrue(loaded.isAvailable(0, 2));
    assertTrue(!loaded.isAvailable(0, 3));
    BlocksDao.remove(teacher.getId(), 0, 3);
    Teacher refreshed = TeacherDao.findOrCreate("Dao Teacher A");
    assertTrue(refreshed.isAvailable(0, 3));
  }

  @Test
  void timetableSaveLoadRoundTrip() throws Exception {
    Subject subject = SubjectDao.findOrCreate("Dao Subject B");
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher B");
    SchoolClass klass = new SchoolClass(subject, teacher);
    klass.setGrade("Grade 11");
    klass.setRoom("202");
    Slot saved = TimetableDao.save(2, 5, klass);
    List<Slot> slots = TimetableDao.slots(null);
    Slot loaded = slots.stream().filter(s -> s.id() == saved.id()).findFirst().orElseThrow();
    assertEquals("Dao Subject B", loaded.subject());
    assertEquals("Dao Teacher B", loaded.teacher());
    assertEquals("202", loaded.room());
    TimetableDao.delete(saved.id());
    slots = TimetableDao.slots(null);
    assertTrue(slots.stream().noneMatch(s -> "Dao Subject B".equals(s.subject())));
  }

  @Test
  void parallelClassesInOneSlotPersistAndClash() throws Exception {
    Subject biology = SubjectDao.findOrCreate("Dao Subject B");
    Subject spanish = SubjectDao.findOrCreate("Dao Subject C");
    Teacher t1 = TeacherDao.findOrCreate("Dao Teacher B");
    Teacher t2 = TeacherDao.findOrCreate("Dao Teacher C");
    Slot b = TimetableDao.save(0, 1, new SchoolClass(biology, t1));
    Slot s = TimetableDao.save(0, 1, new SchoolClass(spanish, t2));
    assertEquals(2, TimetableDao.slots(null).stream()
        .filter(x -> (x.day() == 0 && x.period() == 1) && x.subject().startsWith("Dao"))
        .count(),
        "unexpected rows: " + TimetableDao.slots(null).stream()
            .filter(x -> x.day() == 0 && x.period() == 1)
            .map(x -> x.id() + ":" + x.subject() + "/" + x.teacher() + "/" + x.grade() + "/" + x.room())
            .toList());
    Student student = new Student("Dao Student B", "Grade 11");
    student.addCourse(biology);
    student.addCourse(spanish);
    StudentDao.upsert(student, student.getCourses());
    Map<Integer, List<String>> conflicts = ConflictDetector.detect(
        TimetableDao.slots(null), StudentDao.all(), TeacherDao.all());
    assertTrue(conflicts.containsKey(b.id()));
    assertTrue(conflicts.containsKey(s.id()));
    TimetableDao.delete(b.id());
    TimetableDao.delete(s.id());
  }

  @Test
  void studentCourseLinksRoundTrip() throws Exception {
    Subject s1 = SubjectDao.findOrCreate("Dao Subject C");
    Subject s2 = SubjectDao.findOrCreate("Dao Subject D");
    Student student = new Student("Dao Student A", "Grade 11");
    StudentDao.upsert(student, List.of(s1, s2));
    List<Student> all = StudentDao.all();
    Student loaded = all.stream().filter(st -> st.getName().equals("Dao Student A")).findFirst().orElseThrow();
    assertEquals("Grade 11", loaded.getGrade());
    assertEquals(2, loaded.getCourses().size());
  }

  @Test
  void deletingTeacherReassignsClassesToPlaceholder() throws Exception {
    Subject subject = SubjectDao.findOrCreate("Dao Subject E");
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher D");
    TimetableDao.save(1, 2, new SchoolClass(subject, teacher));
    TimetableDao.reassignToPlaceholder(teacher);
    TeacherDao.delete(teacher);
    List<Slot> after = TimetableDao.slots(null);
    assertTrue(after.stream().noneMatch(s -> s.subject().equals("Dao Subject E")
        && !s.teacher().equals(TimetableDao.UNASSIGNED)));
    assertTrue(TeacherDao.all().stream().noneMatch(t -> t.getName().equals("Dao Teacher D")));
  }
}
