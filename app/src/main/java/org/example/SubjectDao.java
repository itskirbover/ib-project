package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** CRUD for subjects. */
public final class SubjectDao {

  public static List<Subject> all() throws SQLException {
    List<Subject> list = new ArrayList<>();
    try (Connection c = Database.open();
        Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("SELECT id, name, level FROM subjects ORDER BY name")) {
      while (rs.next()) {
        Subject subject = new Subject(rs.getString("name"));
        subject.setId(rs.getInt("id"));
        subject.setLevel(rs.getString("level"));
        list.add(subject);
      }
    }
    return list;
  }

  /** Returns the subject with the given name, creating it if needed. */
  public static Subject findOrCreate(String name) throws SQLException {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Subject name must not be blank");
    }
    String trimmed = name.trim();
    try (Connection c = Database.open()) {
      try (PreparedStatement ps = c.prepareStatement("SELECT id, level FROM subjects WHERE name = ?")) {
        ps.setString(1, trimmed);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            Subject existing = new Subject(trimmed);
            existing.setId(rs.getInt("id"));
            existing.setLevel(rs.getString("level"));
            return existing;
          }
        }
      }
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO subjects(name, level) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, trimmed);
        ps.setString(2, levelFor(trimmed));
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
          keys.next();
          Subject created = new Subject(trimmed);
          created.setId(keys.getInt(1));
          created.setLevel(levelFor(trimmed));
          return created;
        }
      }
    }
  }

  /** Level implied by the last name token (custom names default to SL). */
  static String levelFor(String name) {
    return name != null && name.endsWith(" HL") ? Subject.HL : Subject.SL;
  }

  /**
   * Changes a subject's level (HL = 6 periods/week, SL = 4). Existing
   * timetable sessions are left untouched; the weekly-count conflict flags
   * any mismatch until the schedule is adjusted.
   */
  public static void setLevel(String name, String level) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement(
            "UPDATE subjects SET level = ? WHERE name = ?")) {
      ps.setString(1, Subject.HL.equals(level) ? Subject.HL : Subject.SL);
      ps.setString(2, name == null ? "" : name.trim());
      ps.executeUpdate();
    }
  }

  /** Renames a subject; students, teachers and classes keep their links. */
  public static void rename(String oldName, String newName) throws SQLException {
    String from = oldName == null ? "" : oldName.trim();
    String to = newName == null ? "" : newName.trim();
    if (from.isBlank() || to.isBlank()) {
      throw new IllegalArgumentException("Subject name must not be blank");
    }
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("UPDATE subjects SET name = ?, level = ? WHERE name = ?")) {
      ps.setString(1, to);
      ps.setString(2, levelFor(to));
      ps.setString(3, from);
      ps.executeUpdate();
    }
  }

  /** Deletes a subject; the cascade removes its student, teacher and class links. */
  public static void delete(String name) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("DELETE FROM subjects WHERE name = ?")) {
      ps.setString(1, name == null ? "" : name.trim());
      ps.executeUpdate();
    }
  }

  private SubjectDao() {
  }
}
