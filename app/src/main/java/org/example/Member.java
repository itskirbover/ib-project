package org.example;

import java.util.ArrayList;
import java.util.List;

/** A person at the school: a {@link Student} or a {@link Teacher}. */
public abstract class Member {
  private final String name;
  private final List<Subject> courses = new ArrayList<>();

  protected Member(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Member name must not be null");
    }
    this.name = name.trim();
  }

  public String getName() {
    return name;
  }

  /** The courses this member is linked to (attends or teaches). */
  public List<Subject> getCourses() {
    return courses;
  }

  public boolean isLinkedTo(Subject subject) {
    return courses.contains(subject);
  }

  public void addCourse(Subject subject) {
    if (subject != null && !courses.contains(subject)) {
      courses.add(subject);
    }
  }

  public void removeCourse(Subject subject) {
    courses.remove(subject);
  }

  @Override
  public String toString() {
    return name;
  }
}
