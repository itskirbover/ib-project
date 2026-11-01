package org.example;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Central controller: one window (MainShell) whose content is swapped, plus
 * session caches of students, teachers, slots and current conflicts, all
 * backed by the database and refreshed after every write.
 */
public final class Views {
  static MainShell shell;
  static List<Student> students = new ArrayList<>();
  static List<Teacher> teachers = new ArrayList<>();
  static List<Subject> subjects = new ArrayList<>();
  static List<Slot> slots = new ArrayList<>();
  static Map<Integer, List<String>> conflicts = new HashMap<>();

  private Views() {
  }

  static void init() {
    refreshData();
    shell = new MainShell();
    shell.showHome();
  }

  /** Reloads everything from the database and re-renders the current card. */
  static void refresh() {
    refreshData();
    shell.rerenderCurrent();
  }

  static void refreshData() {
    try {
      students = StudentDao.all();
      teachers = TeacherDao.all();
      subjects = SubjectDao.all();
      slots = TimetableDao.slots(null);
      conflicts = ConflictDetector.detect(slots, students, teachers, subjectLevels());
      Registry.load();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /** Subject name -> HL/SL level map used by the conflict rules. */
  static Map<String, String> subjectLevels() {
    Map<String, String> levels = new HashMap<>();
    for (Subject subject : subjects) {
      levels.put(subject.getName(), subject.getLevel());
    }
    return levels;
  }

  /** The stored level of a subject; unknown or custom subjects count as SL. */
  static boolean isHl(String subjectName) {
    return "HL".equals(subjectLevels().get(subjectName));
  }

  static Teacher teacherByName(String name) {
    return teachers.stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
  }

  static Student studentByName(String name) {
    return students.stream().filter(s -> s.getName().equals(name)).findFirst().orElse(null);
  }

  /** Students enrolled in a subject (their course list contains it). */
  static List<Student> studentsOfSubject(String subjectName) {
    return students.stream()
        .filter(s -> s.getCourses().stream().anyMatch(sub -> sub.getName().equals(subjectName)))
        .toList();
  }

  static List<Slot> slotsOfSubject(String subjectName) {
    return slots.stream().filter(s -> s.subject().equals(subjectName)).toList();
  }

  static List<Slot> slotsOfTeacher(String teacherName) {
    return slots.stream().filter(s -> s.teacher().equals(teacherName)).toList();
  }

  static List<Slot> slotsOfStudent(Student student) {
    return slots.stream()
        .filter(s -> student.getCourses().stream().anyMatch(sub -> sub.getName().equals(s.subject())))
        .toList();
  }
}
