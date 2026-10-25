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
      slots = TimetableDao.slots(null);
      conflicts = ConflictDetector.detect(slots, students, teachers);
      Registry.load();
    } catch (Exception e) {
      e.printStackTrace();
    }
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
