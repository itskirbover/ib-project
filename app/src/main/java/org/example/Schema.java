package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DDL for the schedule database; safe to run on every startup. */
final class Schema {

  /** A legacy/draft single-period row (before session pairing). */
  record LegacyRow(int day, int period, int subjectId, int teacherId,
      String grade, String room) {
  }

  static void initialize(Connection c) throws SQLException {
    // Migration: pre-multi-class timetable_slots had PK(day, period); drop it in
    // favor of one row per class so parallel classes in one slot are representable.
    try (Statement chk = c.createStatement();
        ResultSet hasId = chk.executeQuery("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'timetable_slots'
              AND COLUMN_NAME = 'id'""")) {
      if (hasId.next() && hasId.getInt(1) == 0) {
        try (Statement s = c.createStatement()) {
          s.executeUpdate("DROP TABLE timetable_slots");
        }
      }
    }
    try (Statement s = c.createStatement()) {
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS subjects (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(120) NOT NULL UNIQUE,
            level VARCHAR(8) NOT NULL DEFAULT 'SL'
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS students (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            grade VARCHAR(40) NOT NULL DEFAULT ''
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS student_subjects (
            student_id INT NOT NULL,
            subject_id INT NOT NULL,
            PRIMARY KEY (student_id, subject_id),
            FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
            FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS teachers (
            id INT AUTO_INCREMENT PRIMARY KEY,
            name VARCHAR(120) NOT NULL,
            max_hours INT NOT NULL DEFAULT 0
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS teacher_subjects (
            teacher_id INT NOT NULL,
            subject_id INT NOT NULL,
            PRIMARY KEY (teacher_id, subject_id),
            FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE,
            FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

      reshapeTimetableSlots(s.getConnection());

      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS teacher_unavailability (
            teacher_id INT NOT NULL,
            day INT NOT NULL,
            period INT NOT NULL,
            PRIMARY KEY (teacher_id, day, period),
            FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

      ensureLevelColumn(s.getConnection());
      migrateLegacyToSl(s.getConnection());
      seedIbSubjects(s, s.getConnection());
    }
  }

  /**
   * Every class is stored as one dual session row: (day, start_period) where
   * start_period is an even period index, the session covering periods
   * start_period and start_period+1. Older databases were shaped per single
   * period (column 'period'); their rows are converted to sessions here.
   */
  private static void reshapeTimetableSlots(Connection c) throws SQLException {
    boolean legacyShape;
    try (Statement chk = c.createStatement();
        ResultSet rs = chk.executeQuery("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'timetable_slots'
              AND COLUMN_NAME = 'period'""")) {
      legacyShape = rs.next() && rs.getInt(1) > 0;
    }
    if (!legacyShape) {
      try (Statement s = c.createStatement()) {
        s.executeUpdate("""
            CREATE TABLE IF NOT EXISTS timetable_slots (
              id INT AUTO_INCREMENT PRIMARY KEY,
              day INT NOT NULL,
              start_period INT NOT NULL,
              subject_id INT NOT NULL,
              teacher_id INT NOT NULL,
              grade VARCHAR(40) NOT NULL DEFAULT '',
              room VARCHAR(40) NOT NULL DEFAULT '',
              UNIQUE KEY uniq_class (day, start_period, subject_id, teacher_id),
              CHECK (day BETWEEN 0 AND 4),
              CHECK (start_period BETWEEN 0 AND 6 AND start_period % 2 = 0),
              FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
              FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      }
      return;
    }

    List<LegacyRow> legacy = new ArrayList<>();
    try (Statement s = c.createStatement();
        ResultSet rs = s.executeQuery("""
            SELECT day, period, subject_id, teacher_id, grade, room
            FROM timetable_slots""")) {
      while (rs.next()) {
        legacy.add(new LegacyRow(rs.getInt("day"), rs.getInt("period"),
            rs.getInt("subject_id"), rs.getInt("teacher_id"),
            rs.getString("grade"), rs.getString("room")));
      }
    }

    try (Statement s = c.createStatement()) {
      List<LegacyRow> sessions = pairLegacyRows(legacy);
      s.executeUpdate("""
          CREATE TABLE timetable_sessions_tmp (
            id INT AUTO_INCREMENT PRIMARY KEY,
            day INT NOT NULL,
            start_period INT NOT NULL,
            subject_id INT NOT NULL,
            teacher_id INT NOT NULL,
            grade VARCHAR(40) NOT NULL DEFAULT '',
            room VARCHAR(40) NOT NULL DEFAULT '',
            UNIQUE KEY uniq_class (day, start_period, subject_id, teacher_id),
            CHECK (day BETWEEN 0 AND 4),
            CHECK (start_period BETWEEN 0 AND 6 AND start_period % 2 = 0),
            FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
            FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      try {
        try (PreparedStatement ps = c.prepareStatement(
            "INSERT INTO timetable_sessions_tmp(day, start_period, subject_id, teacher_id, grade, room) VALUES (?, ?, ?, ?, ?, ?)")) {
          for (LegacyRow session : sessions) {
            ps.setInt(1, session.day());
            ps.setInt(2, session.period()); // pair start stored in period
            ps.setInt(3, session.subjectId());
            ps.setInt(4, session.teacherId());
            ps.setString(5, session.grade());
            ps.setString(6, session.room());
            ps.executeUpdate();
          }
        }
        s.executeUpdate("DROP TABLE timetable_slots");
        s.executeUpdate("RENAME TABLE timetable_sessions_tmp TO timetable_slots");
      } catch (SQLException e) {
        s.executeUpdate("DROP TABLE timetable_sessions_tmp");
        throw e;
      }
    }
  }

  /**
   * Groups legacy single-period rows by (day, subject, teacher, grade, room)
   * and merges consecutive periods into aligned dual sessions. Non-pairable
   * leftovers snap into the pair containing their period; duplicates against
   * the session unique key are dropped, cross-subject collisions resurface as
   * ordinary conflicts.
   */
  static List<LegacyRow> pairLegacyRows(List<LegacyRow> legacy) {
    Map<String, List<LegacyRow>> groups = new LinkedHashMap<>();
    for (LegacyRow row : legacy) {
      String key = row.day() + "\u0000" + row.subjectId() + "\u0000"
          + row.teacherId() + "\u0000" + row.grade() + "\u0000" + row.room();
      groups.computeIfAbsent(key, x -> new ArrayList<>()).add(row);
    }
    Map<String, LegacyRow> unique = new LinkedHashMap<>();
    List<LegacyRow> result = new ArrayList<>();
    for (List<LegacyRow> group : groups.values()) {
      group.sort((a, b) -> Integer.compare(a.period(), b.period()));
      int i = 0;
      while (i < group.size()) {
        LegacyRow current = group.get(i);
        int start = current.period() - current.period() % 2;
        if (current.period() % 2 == 0 && i + 1 < group.size()
            && group.get(i + 1).period() == current.period() + 1) {
          i++; // consume the consecutive partner
        }
        String uniqueKey = current.day() + "\u0000" + start + "\u0000"
            + current.subjectId() + "\u0000" + current.teacherId();
        if (unique.putIfAbsent(uniqueKey, current) == null) {
          result.add(new LegacyRow(current.day(), start, current.subjectId(),
              current.teacherId(), current.grade(), current.room()));
        }
        i++;
      }
    }
    return result;
  }

  /** Adds subjects.level on databases created before the level feature. */
  private static void ensureLevelColumn(Connection c) throws SQLException {
    try (Statement chk = c.createStatement();
        ResultSet rs = chk.executeQuery("""
            SELECT COUNT(*) FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'subjects'
              AND COLUMN_NAME = 'level'""")) {
      if (!rs.next()) {
        return;
      }
      if (rs.getInt(1) > 0) {
        return;
      }
    }
    try (Statement s = c.createStatement()) {
      s.executeUpdate(
          "ALTER TABLE subjects ADD COLUMN level VARCHAR(8) NOT NULL DEFAULT 'SL'");
      s.executeUpdate("UPDATE subjects SET level = 'HL' WHERE name LIKE '% HL'");
    }
  }

  /**
   * One-time migration: subjects that used to be listed once (e.g. "Biology")
   * are now split into separate HL and SL entries, so a pre-existing flat row
   * is renamed to its SL variant (keeping every student, teacher and class
   * link) unless the SL row already exists.
   */
  private static void migrateLegacyToSl(Connection c) throws SQLException {
    for (String name : IBSubjects.ALL) {
      if (!name.endsWith(" SL")) {
        continue;
      }
      String base = name.substring(0, name.length() - 3);
      boolean legacyExists = subjectExists(c, base);
      boolean slExists = subjectExists(c, name);
      if (!legacyExists || slExists) {
        continue;
      }
      try (PreparedStatement ps = c.prepareStatement("UPDATE subjects SET name = ? WHERE name = ?")) {
        ps.setString(1, name);
        ps.setString(2, base);
        ps.executeUpdate();
      }
    }
  }

  private static boolean subjectExists(Connection c, String name) throws SQLException {
    try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM subjects WHERE name = ?")) {
      ps.setString(1, name);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    }
  }

  /**
   * Ensures every IB subject exists in the catalog (INSERT IGNORE is idempotent)
   * and stamps HL/SL levels from the name suffix.
   */
  private static void seedIbSubjects(Statement s, Connection c) throws SQLException {
    for (String name : IBSubjects.ALL) {
      try (PreparedStatement ps = c.prepareStatement(
          "INSERT IGNORE INTO subjects(name, level) VALUES (?, ?)")) {
        ps.setString(1, name);
        ps.setString(2, name.endsWith(" HL") ? "HL" : "SL");
        ps.executeUpdate();
      }
    }
  }

  private Schema() {
  }
}
