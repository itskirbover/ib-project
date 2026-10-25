package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Teacher no-time slots ([day, period] pairs per teacher). */
public final class BlocksDao {

  public static List<int[]> forTeacher(int teacherId) throws SQLException {
    try (Connection c = Database.open()) {
      return forTeacher(c, teacherId);
    }
  }

  public static List<int[]> forTeacher(Connection c, int teacherId) throws SQLException {
    List<int[]> blocks = new ArrayList<>();
    try (PreparedStatement ps = c.prepareStatement(
        "SELECT day, period FROM teacher_unavailability WHERE teacher_id = ?")) {
      ps.setInt(1, teacherId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          blocks.add(new int[] { rs.getInt("day"), rs.getInt("period") });
        }
      }
    }
    return blocks;
  }

  public static void add(int teacherId, int day, int period) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement(
            "INSERT IGNORE INTO teacher_unavailability(teacher_id, day, period) VALUES (?, ?, ?)")) {
      ps.setInt(1, teacherId);
      ps.setInt(2, day);
      ps.setInt(3, period);
      ps.executeUpdate();
    }
  }

  public static void remove(int teacherId, int day, int period) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement(
            "DELETE FROM teacher_unavailability WHERE teacher_id = ? AND day = ? AND period = ?")) {
      ps.setInt(1, teacherId);
      ps.setInt(2, day);
      ps.setInt(3, period);
      ps.executeUpdate();
    }
  }

  private BlocksDao() {
  }
}
