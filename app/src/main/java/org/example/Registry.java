package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Session-level catalog of subjects, teachers and grade levels that feeds the
 * schedule editor's drop-downs. It is seeded from the loaded schedule and
 * grows as the user adds entries through the "Add subject/teacher..." actions.
 */
public final class Registry {
  public static final String ADD_SUBJECT = "+ Add subject...";
  public static final String ADD_TEACHER = "+ Add teacher...";

  private static final List<String> subjects = new ArrayList<>();
  private static final List<String> teachers = new ArrayList<>();
  private static final List<String> grades = new ArrayList<>(List.of("Grade 10", "Grade 11", "Grade 12"));

  private Registry() {
  }

  public static void register(String subject, String teacher, String grade) {
    if (subject != null && !subject.isBlank()) {
      addSubject(subject.trim());
    }
    if (teacher != null && !teacher.isBlank()) {
      addTeacher(teacher.trim());
    }
    if (grade != null && !grade.isBlank()) {
      addUnique(grades, grade.trim());
    }
  }

  public static void addSubject(String name) {
    addUnique(subjects, name);
  }

  public static void addTeacher(String name) {
    addUnique(teachers, name);
  }

  public static List<String> subjectItems() {
    return items(subjects, ADD_SUBJECT);
  }

  public static List<String> teacherItems() {
    return items(teachers, ADD_TEACHER);
  }

  public static List<String> gradeItems() {
    return new ArrayList<>(grades);
  }

  private static void addUnique(List<String> list, String value) {
    if (value != null && !value.isBlank() && !list.contains(value.trim())) {
      list.add(value.trim());
    }
  }

  private static List<String> items(List<String> values, String addEntry) {
    List<String> list = new ArrayList<>(values);
    list.add(addEntry);
    return list;
  }
}
