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
        ResultSet rs = s.executeQuery("SELECT id, name FROM subjects ORDER BY name")) {
      while (rs.next()) {
        Subject subject = new Subject(rs.getString("name"));
        subject.setId(rs.getInt("id"));
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
      try (PreparedStatement ps = c.prepareStatement("SELECT id FROM subjects WHERE name = ?")) {
        ps.setString(1, trimmed);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            Subject existing = new Subject(trimmed);
            existing.setId(rs.getInt("id"));
            return existing;
          }
        }
      }
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO subjects(name) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
        ps.setString(1, trimmed);
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
          keys.next();
          Subject created = new Subject(trimmed);
          created.setId(keys.getInt(1));
          return created;
        }
      }
    }
  }

  private SubjectDao() {
  }
}
