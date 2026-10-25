package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** CRUD for students (their Subject[] list of courses lives in student_subjects). */
public final class StudentDao {

  public static List<Student> all() throws SQLException {
    List<Student> list = new ArrayList<>();
    try (Connection c = Database.open();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT id, name, grade FROM students ORDER BY name")) {
      while (rs.next()) {
        Student student = new Student(rs.getString("name"), rs.getString("grade"));
        student.setId(rs.getInt("id"));
        loadCourses(c, student);
        list.add(student);
      }
    }
    return list;
  }

  /** Inserts or updates the student and replaces their course links in one transaction. */
  public static void upsert(Student student, List<Subject> subjects) throws SQLException {
    try (Connection c = Database.open()) {
      c.setAutoCommit(false);
      try {
        int id;
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE students SET name = ?, grade = ? WHERE id = ?")) {
          ps.setString(1, student.getName());
          ps.setString(2, student.getGrade());
          ps.setInt(3, student.getId());
          int updated = ps.executeUpdate();
          if (updated == 0) {
            try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO students(name, grade) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
              ins.setString(1, student.getName());
              ins.setString(2, student.getGrade());
              ins.executeUpdate();
              try (ResultSet keys = ins.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
              }
            }
          } else {
            id = student.getId();
          }
        }
        student.setId(id);
        try (PreparedStatement del = c.prepareStatement(
            "DELETE FROM student_subjects WHERE student_id = ?")) {
          del.setInt(1, id);
          del.executeUpdate();
        }
        try (PreparedStatement link = c.prepareStatement(
            "INSERT INTO student_subjects(student_id, subject_id) VALUES (?, ?)")) {
          for (Subject subject : subjects) {
            link.setInt(1, id);
            link.setInt(2, subject.getId());
            link.addBatch();
          }
          link.executeBatch();
        }
        c.commit();
      } catch (SQLException e) {
        c.rollback();
        throw e;
      } finally {
        c.setAutoCommit(true);
      }
    }
  }

  private static void loadCourses(Connection c, Student student) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("""
        SELECT su.id, su.name FROM student_subjects ss
        JOIN subjects su ON su.id = ss.subject_id
        WHERE ss.student_id = ? ORDER BY su.name""")) {
      ps.setInt(1, student.getId());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Subject subject = new Subject(rs.getString("name"));
          subject.setId(rs.getInt("id"));
          student.addCourse(subject);
        }
      }
    }
  }

  private StudentDao() {
  }
}
