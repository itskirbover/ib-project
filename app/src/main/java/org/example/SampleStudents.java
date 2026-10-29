package org.example;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Replaces the roster of one subject with a generated sample set. The students
 * currently taking the subject are deleted; fresh students take the subject
 * plus filler courses, and every third one takes a second subject whose class
 * shares the slot, producing genuine conflicts. If no other subject's class
 * shares the slot, one parallel class (No teacher placeholder) is created so
 * collisions become possible at all.
 */
public final class SampleStudents {

  private static final String[] NAMES = {
      "Ana Souza", "Ben Carter", "Carla Reyes", "Dima Volkov", "Elif Kaya",
      "Farah Nasser", "Gustav Lund", "Hana Sato", "Ivan Petrov", "June Park",
      "Lena Fischer", "Marta Okafor"
  };

  private static final String[] GRADES = { "Grade 10", "Grade 11", "Grade 12" };

  /** Every third generated student takes the clashing subject as well. */
  private static final int CONFLICT_EVERY = 3;

  /**
   * Deletes the subject's current students and persists {@code size} sample
   * students; returns the generated roster.
   */
  public static List<Student> replaceRoster(Slot slot, int size) throws SQLException {
    String subjectName = slot.subject();

    for (Student current : Views.studentsOfSubject(subjectName)) {
      StudentDao.delete(current);
    }
    Views.refreshData(); // fresh cache for uniqueness checks and slot lookups

    // Subjects whose classes share this slot: taking any of them clashes.
    Set<String> inCellSubjects = new LinkedHashSet<>();
    for (Slot other : Views.slots) {
      if (other.day() == slot.day() && other.period() == slot.period()) {
        inCellSubjects.add(other.subject());
      }
    }
    String clashSubject;
    if (inCellSubjects.size() <= 1) {
      clashSubject = firstOtherSubject(subjectName);
      TimetableDao.save(slot.day(), slot.period(),
          new SchoolClass(new Subject(clashSubject), new Teacher(TimetableDao.UNASSIGNED)));
    } else {
      clashSubject = inCellSubjects.stream()
          .filter(s -> !s.equals(subjectName))
          .findFirst()
          .orElseThrow();
    }

    List<String> taken = Views.students.stream().map(Student::getName).toList();
    List<Student> roster = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      Student student = new Student(uniqueName(NAMES[i % NAMES.length], taken),
          GRADES[i % GRADES.length]);
      StudentDao.upsert(student, resolveCourses(coursePlan(i, subjectName, clashSubject,
          inCellSubjects)));
      roster.add(student);
    }
    return roster;
  }

  /** Course name list for student #i: the subject, fillers, clash for conflicts. */
  static List<String> coursePlan(int i, String subjectName, String clashSubject,
      Set<String> inCellSubjects) {
    List<String> pool = new ArrayList<>();
    for (String name : IBSubjects.ALL) {
      if (!name.equals(subjectName) && !inCellSubjects.contains(name)) {
        pool.add(name);
      }
    }
    List<String> rotated = new ArrayList<>();
    int start = (i * 2) % pool.size();
    for (int k = 0; k < pool.size(); k++) {
      rotated.add(pool.get((start + k) % pool.size()));
    }
    // Subject + up to 4 fillers (and the clash subject for conflict rows) = 6 courses max.
    int slotsLeft = 5 - (conflicting(i) ? 1 : 0);
    int fillerCount = Math.min(4, Math.min(pool.size(), slotsLeft));
    Set<String> plan = new LinkedHashSet<>();
    for (int k = 0; k < fillerCount; k++) {
      plan.add(rotated.get(k));
    }
    if (conflicting(i)) {
      plan.add(clashSubject);
    }
    plan.add(subjectName);
    return new ArrayList<>(plan);
  }

  /** True when student #i is meant to clash with the slot. */
  static boolean conflicting(int i) {
    return i % CONFLICT_EVERY == 2;
  }

  private static List<Subject> resolveCourses(List<String> courseNames) throws SQLException {
    List<Subject> resolved = new ArrayList<>();
    for (String name : courseNames) {
      resolved.add(SubjectDao.findOrCreate(name));
    }
    return resolved;
  }

  private static String uniqueName(String base, List<String> taken) {
    String name = base;
    int suffix = 2;
    while (taken.contains(name)) {
      name = base + " " + suffix++;
    }
    return name;
  }

  private static String firstOtherSubject(String subjectName) {
    for (String name : IBSubjects.ALL) {
      if (!name.equals(subjectName)) {
        return name;
      }
    }
    throw new IllegalStateException("No other subject exists in the catalog");
  }

  private SampleStudents() {
  }
}
