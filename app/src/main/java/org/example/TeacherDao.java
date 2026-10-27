package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** CRUD for teachers (courses in teacher_subjects, no-time slots in teacher_unavailability). */
public final class TeacherDao {

  public static List<Teacher> all() throws SQLException {
    List<Teacher> list = new ArrayList<>();
    try (Connection c = Database.open();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT id, name, max_hours FROM teachers ORDER BY name")) {
      while (rs.next()) {
        Teacher teacher = new Teacher(rs.getString("name"));
        teacher.setId(rs.getInt("id"));
        teacher.setMaxHoursPerWeek(rs.getInt("max_hours"));
        loadCourses(c, teacher);
        loadBlocks(c, teacher);
        list.add(teacher);
      }
    }
    return list;
  }

  /** Returns the teacher with the given name, creating it if needed. */
  public static Teacher findOrCreate(String name) throws SQLException {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Teacher name must not be blank");
    }
    String trimmed = name.trim();
    try (Connection c = Database.open()) {
      try (PreparedStatement ps = c.prepareStatement("SELECT id, max_hours FROM teachers WHERE name = ?")) {
        ps.setString(1, trimmed);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            Teacher existing = new Teacher(trimmed);
            existing.setId(rs.getInt("id"));
            existing.setMaxHoursPerWeek(rs.getInt("max_hours"));
            return existing;
          }
        }
      }
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO teachers(name, max_hours) VALUES (?, 0)", Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, trimmed);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
          keys.next();
          Teacher created = new Teacher(trimmed);
          created.setId(keys.getInt(1));
          created.setMaxHoursPerWeek(0);
          return created;
        }
      }
    }
  }

  /** Deletes a teacher; their classes are first reassigned to "(No teacher)". */
  public static void delete(Teacher teacher) throws SQLException {
    TimetableDao.reassignToPlaceholder(teacher);
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("DELETE FROM teachers WHERE id = ?")) {
      ps.setInt(1, teacher.getId());
      ps.executeUpdate();
    }
  }

  /** Inserts or updates the teacher plus their subject links and no-time slots. */
  public static void upsert(Teacher teacher, List<Subject> subjects, List<int[]> blocks)
      throws SQLException {
    try (Connection c = Database.open()) {
      c.setAutoCommit(false);
      try {
        int storedHours = teacher.getMaxHoursPerWeek() == Integer.MAX_VALUE ? 0 : teacher.getMaxHoursPerWeek();
        int id;
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE teachers SET name = ?, max_hours = ? WHERE id = ?")) {
          ps.setString(1, teacher.getName());
          ps.setInt(2, storedHours);
          ps.setInt(3, teacher.getId());
          int updated = ps.executeUpdate();
          if (updated == 0) {
            try (PreparedStatement ins = c.prepareStatement(
                "INSERT INTO teachers(name, max_hours) VALUES (?, ?)",
                Statement.RETURN_GENERATED_KEYS)) {
              ins.setString(1, teacher.getName());
              ins.setInt(2, storedHours);
              ins.executeUpdate();
              try (ResultSet keys = ins.getGeneratedKeys()) {
                keys.next();
                id = keys.getInt(1);
              }
            }
          } else {
            id = teacher.getId();
          }
        }
        teacher.setId(id);
        try (PreparedStatement del = c.prepareStatement(
            "DELETE FROM teacher_subjects WHERE teacher_id = ?")) {
          del.setInt(1, id);
          del.executeUpdate();
        }
        try (PreparedStatement link = c.prepareStatement(
            "INSERT INTO teacher_subjects(teacher_id, subject_id) VALUES (?, ?)")) {
          for (Subject subject : subjects) {
            link.setInt(1, id);
            link.setInt(2, subject.getId());
            link.addBatch();
          }
          link.executeBatch();
        }
        try (PreparedStatement del = c.prepareStatement(
            "DELETE FROM teacher_unavailability WHERE teacher_id = ?")) {
          del.setInt(1, id);
          del.executeUpdate();
        }
        try (PreparedStatement block = c.prepareStatement(
            "INSERT INTO teacher_unavailability(teacher_id, day, period) VALUES (?, ?, ?)")) {
          for (int[] b : blocks) {
            block.setInt(1, id);
            block.setInt(2, b[0]);
            block.setInt(3, b[1]);
            block.addBatch();
          }
          block.executeBatch();
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

  private static void loadCourses(Connection c, Teacher teacher) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("""
        SELECT s.id, s.name FROM teacher_subjects ts
        JOIN subjects s ON s.id = ts.subject_id
        WHERE ts.teacher_id = ? ORDER BY s.name""")) {
      ps.setInt(1, teacher.getId());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Subject subject = new Subject(rs.getString("name"));
          subject.setId(rs.getInt("id"));
          teacher.addCourse(subject);
        }
      }
    }
  }

  private static void loadBlocks(Connection c, Teacher teacher) throws SQLException {
    teacher.installBlocks(BlocksDao.forTeacher(c, teacher.getId()));
  }

  private TeacherDao() {
  }
}
