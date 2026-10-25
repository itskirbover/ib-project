package org.example;

/** A student taking up to six IB DP courses. */
public class Student extends Member {
  public static final int MAX_COURSES = 6;

  private final String grade;

  public Student(String name, String grade) {
    super(name);
    this.grade = grade == null ? "" : grade.trim();
  }

  public String getGrade() {
    return grade;
  }

  @Override
  public void addCourse(Subject subject) {
    if (subject == null) {
      return;
    }
    if (!isLinkedTo(subject) && getCourses().size() >= MAX_COURSES) {
      throw new IllegalStateException(
          "Student " + getName() + " cannot take more than " + MAX_COURSES + " courses");
    }
    super.addCourse(subject);
  }

  @Override
  public String toString() {
    return getName() + (grade.isEmpty() ? " (Grade)" : " (" + grade + ")");
  }
}
