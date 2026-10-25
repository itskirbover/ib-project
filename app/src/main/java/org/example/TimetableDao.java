package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The timetable: every class is one row via (day, period, subject, teacher, grade, room).
 * Several parallel classes may share one (day, period).
 */
public final class TimetableDao {

  /** All occupied classes, optionally filtered by one subject's name (null = all). */
  public static List<Slot> slots(String subjectName) throws SQLException {
    List<Slot> list = new ArrayList<>();
    String sql = """
        SELECT s.id, s.day, s.period, su.name AS subject, t.name AS teacher,
               s.grade, s.room
        FROM timetable_slots s
        JOIN subjects su ON su.id = s.subject_id
        JOIN teachers t ON t.id = s.teacher_id""";
    if (subjectName != null) {
      sql += " WHERE su.name = ?";
    }
    sql += " ORDER BY s.day, s.period, su.name";
    try (Connection c = Database.open()) {
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        if (subjectName != null) {
          ps.setString(1, subjectName);
        }
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            list.add(new Slot(rs.getInt("id"), rs.getInt("day"), rs.getInt("period"),
                rs.getString("subject"), rs.getString("teacher"),
                rs.getString("grade"), rs.getString("room")));
          }
        }
      }
    }
    return list;
  }

  /**
   * Puts a class into a slot: any existing class of the same subject at the same
   * (day, period) is replaced; other classes in that slot are left alone.
   */
  public static Slot save(int day, int period, SchoolClass klass) throws SQLException {
    if (klass == null || klass.isEmpty()) {
      throw new IllegalArgumentException("Cannot save an empty class");
    }
    try (Connection c = Database.open()) {
      Subject subject = SubjectDao.findOrCreate(klass.getSubject().getName());
      Teacher teacher = TeacherDao.findOrCreate(klass.getTeacher().getName());
      int id;
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO timetable_slots(day, period, subject_id, teacher_id, grade, room) VALUES (?, ?, ?, ?, ?, ?)",
          java.sql.Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, day);
        ps.setInt(2, period);
        ps.setInt(3, subject.getId());
        ps.setInt(4, teacher.getId());
        ps.setString(5, klass.getGrade());
        ps.setString(6, klass.getRoom());
        ps.executeUpdate();
        try (ResultSet keys = ps.getGeneratedKeys()) {
          keys.next();
          id = keys.getInt(1);
        }
      }
      return new Slot(id, day, period, subject.getName(), teacher.getName(),
          klass.getGrade(), klass.getRoom());
    }
  }

  /** Replaces grade and room fields of one class row. */
  public static void updateDetails(Slot slot, String grade, String room) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement(
            "UPDATE timetable_slots SET grade = ?, room = ? WHERE id = ?")) {
      ps.setString(1, grade);
      ps.setString(2, room);
      ps.setInt(3, slot.id());
      ps.executeUpdate();
    }
  }

  /** Removes one class row from a slot. */
  public static void delete(int slotId) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("DELETE FROM timetable_slots WHERE id = ?")) {
      ps.setInt(1, slotId);
      ps.executeUpdate();
    }
  }

  /** Removes every class of a subject in a slot (used by "replace subject in cell"). */
  public static void deleteSubjectInSlot(int day, int period, String subjectName) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("""
            DELETE s FROM timetable_slots s
            JOIN subjects su ON su.id = s.subject_id
            WHERE s.day = ? AND s.period = ? AND su.name = ?""")) {
      ps.setInt(1, day);
      ps.setInt(2, period);
      ps.setString(3, subjectName);
      ps.executeUpdate();
    }
  }

  private TimetableDao() {
  }
}
