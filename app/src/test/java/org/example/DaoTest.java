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
    assertTrue(all.stream().anyMatch(s -> s.getName().equals("Mathematics: Analysis and Approaches SL")));
    assertTrue(all.stream().anyMatch(s -> s.getName().equals("English A: Literature HL")));
    assertTrue(all.stream().anyMatch(s -> s.getName().equals("French ab initio")));
  }

  @Test
  void subjectRenameKeepsLinks() throws Exception {
    Subject subject = SubjectDao.findOrCreate("Dao Subject F");
    Student student = new Student("Dao Student C", "Grade 11");
    StudentDao.upsert(student, List.of(subject));
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher E");
    Slot classSlot = TimetableDao.save(0, 6, new SchoolClass(subject, teacher));
    SubjectDao.rename("Dao Subject F", "Dao Subject F Renamed");

    List<Student> students = StudentDao.all();
    Student reloaded = students.stream()
        .filter(s -> s.getName().equals("Dao Student C"))
        .findFirst().orElseThrow();
    assertTrue(reloaded.getCourses().stream().anyMatch(s -> s.getName().equals("Dao Subject F Renamed")));
    assertTrue(TimetableDao.slots(null).stream()
        .anyMatch(s -> s.id() == classSlot.id() && s.subject().equals("Dao Subject F Renamed")));

    SubjectDao.delete("Dao Subject F Renamed");
  }

  @Test
  void subjectDeleteCascadesLinks() throws Exception {
    Subject subject = SubjectDao.findOrCreate("Dao Subject G");
    Student student = new Student("Dao Student D", "Grade 12");
    StudentDao.upsert(student, List.of(subject));
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher F");
    Slot classSlot = TimetableDao.save(1, 0, new SchoolClass(subject, teacher));

    SubjectDao.delete("Dao Subject G");

    assertTrue(SubjectDao.all().stream().noneMatch(s -> s.getName().equals("Dao Subject G")));
    Student reloaded = StudentDao.all().stream()
        .filter(s -> s.getName().equals("Dao Student D"))
        .findFirst().orElseThrow();
    assertTrue(reloaded.getCourses().stream().noneMatch(s -> s.getName().startsWith("Dao Subject G")));
    assertTrue(TimetableDao.slots(null).stream().noneMatch(s -> s.id() == classSlot.id()));
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
    Slot saved = TimetableDao.save(2, 5, klass); // odd period 5 snaps to session 4-5
    List<Slot> slots = TimetableDao.slots(null);
    List<Slot> copies = slots.stream().filter(s -> s.id() == saved.id()).toList();
    assertEquals(2, copies.size(), "a session appears as two period slots");
    assertEquals(4, copies.get(0).period());
    assertEquals(5, copies.get(1).period());
    Slot loaded = copies.get(0);
    assertEquals("Dao Subject B", loaded.subject());
    assertEquals("Dao Teacher B", loaded.teacher());
    assertEquals("202", loaded.room());
    TimetableDao.delete(saved.id());
    slots = TimetableDao.slots(null);
    assertTrue(slots.stream().noneMatch(s -> "Dao Subject B".equals(s.subject())));
  }

  @Test
  void sessionDetailsAndMoveApplyToBothPeriods() throws Exception {
    Subject subject = SubjectDao.findOrCreate("Dao Subject H");
    Teacher teacher = TeacherDao.findOrCreate("Dao Teacher G");
    Slot saved = TimetableDao.save(3, 3, new SchoolClass(subject, teacher)); // 2-3
    TimetableDao.updateDetails(saved, "Grade 11", "3-15");

    List<Slot> copies = TimetableDao.slots(null).stream()
        .filter(s -> s.id() == saved.id()).toList();
    assertEquals(2, copies.size());
    for (Slot copy : copies) {
      assertEquals("Grade 11", copy.grade());
      assertEquals("3-15", copy.room());
    }

    TimetableDao.move(saved.id(), 4, 1); // picks period 2 -> session 0-1
    copies = TimetableDao.slots(null).stream()
        .filter(s -> s.id() == saved.id()).toList();
    assertEquals(2, copies.size());
    assertEquals(0, copies.get(0).period());
    assertEquals(1, copies.get(1).period());
    assertEquals(4, copies.get(0).day());

    // The data stays identical on both halves after every mutation.
    assertEquals(copies.get(0).room(), copies.get(1).room());
    assertEquals(copies.get(0).subject(), copies.get(1).subject());
    assertEquals(copies.get(0).teacher(), copies.get(1).teacher());
    TimetableDao.delete(saved.id());
  }

  @Test
  void subjectLevelRoundTripsAndBackfillsFromName() throws Exception {
    Subject created = SubjectDao.findOrCreate("Dao Level Custom");
    assertEquals(Subject.SL, created.getLevel()); // custom names without suffix default to SL
    SubjectDao.setLevel("Dao Level Custom", Subject.HL);
    Subject reloaded = SubjectDao.all().stream()
        .filter(s -> s.getName().equals("Dao Level Custom")).findFirst().orElseThrow();
    assertEquals(Subject.HL, reloaded.getLevel());
    assertEquals(6, reloaded.requiredPeriods());
    assertEquals(3, reloaded.requiredSessions());
    SubjectDao.setLevel("Dao Level Custom", Subject.SL);

    Subject fromName = SubjectDao.findOrCreate("Dao Level Suffix HL");
    assertEquals(Subject.HL, fromName.getLevel());
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
