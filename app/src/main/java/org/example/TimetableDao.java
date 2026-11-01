package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The timetable: every class occupies one dual session (two consecutive
 * periods, 0-based starts 0/2/4/6) stored as a single row keyed by
 * (day, start_period, subject, teacher, grade, room). Several parallel
 * classes may share one session. {@link #slots} expands every row into two
 * {@link Slot} objects (one per period) that share the row id, so per-cell
 * UI/analysis code keeps working while all writes stay session-grained.
 */
public final class TimetableDao {

  /** Placeholder for classes whose teacher is not set yet. */
  public static final String UNASSIGNED = "(No teacher)";

  /** Snaps any period to the even start of the session that contains it. */
  public static int sessionStart(int period) {
    return Palette.sessionStart(period);
  }

  /** All occupied sessions (as per-period slots), optionally filtered by subject name. */
  public static List<Slot> slots(String subjectName) throws SQLException {
    List<Slot> list = new ArrayList<>();
    String sql = """
        SELECT s.id, s.day, s.start_period, su.name AS subject, t.name AS teacher,
               s.grade, s.room
        FROM timetable_slots s
        JOIN subjects su ON su.id = s.subject_id
        JOIN teachers t ON t.id = s.teacher_id""";
    if (subjectName != null) {
      sql += " WHERE su.name = ?";
    }
    sql += " ORDER BY s.day, s.start_period, su.name";
    try (Connection c = Database.open()) {
      try (PreparedStatement ps = c.prepareStatement(sql)) {
        if (subjectName != null) {
          ps.setString(1, subjectName);
        }
        try (ResultSet rs = ps.executeQuery()) {
          while (rs.next()) {
            expand(list, rs.getInt("id"), rs.getInt("day"), rs.getInt("start_period"),
                rs.getString("subject"), rs.getString("teacher"),
                rs.getString("grade"), rs.getString("room"));
          }
        }
      }
    }
    return list;
  }

  private static void expand(List<Slot> list, int id, int day, int start, String subject,
      String teacher, String grade, String room) {
    list.add(new Slot(id, day, start, subject, teacher, grade, room));
    list.add(new Slot(id, day, start + 1, subject, teacher, grade, room));
  }

  /**
   * Places a class as a session covering the pair containing {@code period}.
   * A blank teacher name stores the class under the "(No teacher)" placeholder
   * until a teacher claims the subject. A previous identical class in that
   * session is replaced; other parallel classes stay.
   */
  public static Slot save(int day, int period, SchoolClass klass) throws SQLException {
    if (klass == null || klass.isEmpty()) {
      throw new IllegalArgumentException("Cannot save an empty class");
    }
    int start = sessionStart(period);
    String teacherName = klass.getTeacher().getName();
    if (teacherName == null || teacherName.isBlank()) {
      teacherName = UNASSIGNED;
    }
    try (Connection c = Database.open()) {
      Subject subject = SubjectDao.findOrCreate(klass.getSubject().getName());
      Teacher teacher = TeacherDao.findOrCreate(teacherName);
      try (PreparedStatement ps = c.prepareStatement(
          "DELETE FROM timetable_slots WHERE day = ? AND start_period = ? AND subject_id = ? AND teacher_id = ?")) {
        ps.setInt(1, day);
        ps.setInt(2, start);
        ps.setInt(3, subject.getId());
        ps.setInt(4, teacher.getId());
        ps.executeUpdate();
      }
      int id;
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT INTO timetable_slots(day, start_period, subject_id, teacher_id, grade, room) VALUES (?, ?, ?, ?, ?, ?)",
          java.sql.Statement.RETURN_GENERATED_KEYS)) {
        ps.setInt(1, day);
        ps.setInt(2, start);
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
      String grade = klass.getGrade();
      String room = klass.getRoom();
      return new Slot(id, day, start, subject.getName(), teacher.getName(),
          grade, room);
    }
  }

  /**
   * Moves one session (both periods) to the pair containing {@code period}.
   * An identical (subject + teacher) session already sitting there is replaced,
   * mirroring save(). Throws if the row vanished or the update violates the
   * schema CHECK constraints.
   */
  public static void move(int slotId, int day, int period) throws SQLException {
    int start = sessionStart(period);
    try (Connection c = Database.open()) {
      c.setAutoCommit(false);
      try {
        int[] ids;
        try (PreparedStatement ps = c.prepareStatement(
            "SELECT subject_id, teacher_id FROM timetable_slots WHERE id = ?")) {
          ps.setInt(1, slotId);
          try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
              throw new SQLException("Class no longer exists");
            }
            ids = new int[] { rs.getInt(1), rs.getInt(2) };
          }
        }
        try (PreparedStatement ps = c.prepareStatement("""
            DELETE FROM timetable_slots WHERE day = ? AND start_period = ?
              AND subject_id = ? AND teacher_id = ? AND id <> ?""")) {
          ps.setInt(1, day);
          ps.setInt(2, start);
          ps.setInt(3, ids[0]);
          ps.setInt(4, ids[1]);
          ps.setInt(5, slotId);
          ps.executeUpdate();
        }
        try (PreparedStatement ps = c.prepareStatement(
            "UPDATE timetable_slots SET day = ?, start_period = ? WHERE id = ?")) {
          ps.setInt(1, day);
          ps.setInt(2, start);
          ps.setInt(3, slotId);
          ps.executeUpdate();
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

  /** Replaces grade and room fields of one session (same room on both periods). */
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

  /** Removes one session (both periods). */
  public static void delete(int slotId) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("DELETE FROM timetable_slots WHERE id = ?")) {
      ps.setInt(1, slotId);
      ps.executeUpdate();
    }
  }

  /** Removes every session of a subject in the session containing (day, period). */
  public static void deleteSubjectInSlot(int day, int period, String subjectName) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("""
            DELETE s FROM timetable_slots s
            JOIN subjects su ON su.id = s.subject_id
            WHERE s.day = ? AND s.start_period = ? AND su.name = ?""")) {
      ps.setInt(1, day);
      ps.setInt(2, sessionStart(period));
      ps.setString(3, subjectName);
      ps.executeUpdate();
    }
  }

  /** Moves every class currently assigned to a teacher to the "(No teacher)" placeholder. */
  public static void reassignToPlaceholder(Teacher teacher) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement(
            "UPDATE timetable_slots SET teacher_id = ? WHERE teacher_id = ?")) {
      ps.setInt(1, TeacherDao.findOrCreate(UNASSIGNED).getId());
      ps.setInt(2, teacher.getId());
      ps.executeUpdate();
    }
  }

  /**
   * Auto-assignment: moves every "(No teacher)" session of a subject to a teacher
   * who just started teaching that subject.
   */
  public static void reassignUnassigned(String subjectName, Teacher teacher) throws SQLException {
    try (Connection c = Database.open();
        PreparedStatement ps = c.prepareStatement("""
            UPDATE timetable_slots s
            JOIN subjects su ON su.id = s.subject_id
            JOIN teachers ph ON ph.id = s.teacher_id
            SET s.teacher_id = ?
            WHERE su.name = ? AND ph.name = ?""")) {
      ps.setInt(1, teacher.getId());
      ps.setString(2, subjectName);
      ps.setString(3, UNASSIGNED);
      ps.executeUpdate();
    }
  }

  private TimetableDao() {
  }
}
