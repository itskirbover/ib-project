package org.example;

import java.util.ArrayList;
import java.util.List;

/**
 * Session-level catalog of subjects, teachers and grade levels that feeds the
 * editor's drop-downs. On startup it is loaded from the database; the
 * "Add subject/teacher..." actions write through to the database.
 */
public final class Registry {
  public static final String ADD_SUBJECT = "+ Add subject...";
  public static final String ADD_TEACHER = "+ Add teacher...";

  private static final List<String> subjects = new ArrayList<>();
  private static final List<String> teachers = new ArrayList<>();
  private static final List<String> grades = new ArrayList<>(List.of("Grade 10", "Grade 11", "Grade 12"));

  private Registry() {
  }

  /** Seeds the session lists from the database (safe fallback if DB is down). */
  public static void load() {
    try {
      subjects.clear();
      for (Subject s : SubjectDao.all()) {
        subjects.add(s.getName());
      }
      teachers.clear();
      for (Teacher t : TeacherDao.all()) {
        teachers.add(t.getName());
      }
    } catch (Exception e) {
      System.err.println("Registry load failed: " + e.getMessage());
    }
  }

  /** Adds a subject to the catalog and persists it. */
  public static void addSubject(String name) {
    addUnique(subjects, name);
    try {
      SubjectDao.findOrCreate(name);
    } catch (Exception e) {
      System.err.println("Subject persist failed: " + e.getMessage());
    }
  }

  /** Adds a teacher to the catalog and persists it. */
  public static void addTeacher(String name) {
    addUnique(teachers, name);
    try {
      TeacherDao.findOrCreate(name);
    } catch (Exception e) {
      System.err.println("Teacher persist failed: " + e.getMessage());
    }
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
