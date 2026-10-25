package org.example;

/**
 * A scheduled class: one lesson of a {@link Subject}, taught by exactly one
 * {@link Teacher}, placed in a timetable slot (period + time) and room.
 */
public class SchoolClass {
  private final Subject subject;
  private final Teacher teacher;
  private String time;
  private String grade;
  private String room;

  public SchoolClass(Subject subject, Teacher teacher) {
    if (subject == null) {
      throw new IllegalArgumentException("A class must belong to a subject");
    }
    if (teacher == null) {
      throw new IllegalArgumentException("A class must contain exactly one teacher");
    }
    this.subject = subject;
    this.teacher = teacher;
    this.time = "";
    this.grade = "";
    this.room = "";
  }

  public Subject getSubject() {
    return subject;
  }

  public Teacher getTeacher() {
    return teacher;
  }

  public String getTime() {
    return time;
  }

  public void setTime(String time) {
    this.time = time == null ? "" : time.trim();
  }

  public String getGrade() {
    return grade;
  }

  public void setGrade(String grade) {
    this.grade = grade == null ? "" : grade.trim();
  }

  public String getRoom() {
    return room;
  }

  public void setRoom(String room) {
    this.room = room == null ? "" : room.trim();
  }

  public boolean isEmpty() {
    return subject.getName().isEmpty();
  }

  @Override
  public String toString() {
    return subject.getName() + (teacher.getName().isEmpty() ? "" : " (" + teacher.getName() + ")");
  }
}
