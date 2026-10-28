package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** DDL for the schedule database; safe to run on every startup. */
final class Schema {
  static void initialize(Connection c) throws SQLException {
    // Migration: pre-multi-class timetable_slots had PK(day, period); drop it in
    // favor
    // of one row per class so parallel classes in one slot are representable.
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
            name VARCHAR(120) NOT NULL UNIQUE
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
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS timetable_slots (
            id INT AUTO_INCREMENT PRIMARY KEY,
            day INT NOT NULL,
            period INT NOT NULL,
            subject_id INT NOT NULL,
            teacher_id INT NOT NULL,
            grade VARCHAR(40) NOT NULL DEFAULT '',
            room VARCHAR(40) NOT NULL DEFAULT '',
            UNIQUE KEY uniq_class (day, period, subject_id, teacher_id),
            CHECK (day BETWEEN 0 AND 4),
            CHECK (period BETWEEN 0 AND 7),
            FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE,
            FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");
      s.executeUpdate("""
          CREATE TABLE IF NOT EXISTS teacher_unavailability (
            teacher_id INT NOT NULL,
            day INT NOT NULL,
            period INT NOT NULL,
            PRIMARY KEY (teacher_id, day, period),
            FOREIGN KEY (teacher_id) REFERENCES teachers(id) ON DELETE CASCADE
          ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4""");

      seedIbSubjects(s);
    }
  }

  /**
   * Ensures every IB subject exists in the catalog (INSERT IGNORE is idempotent).
   */
  private static void seedIbSubjects(Statement s) throws SQLException {
    for (String name : IBSubjects.ALL) {
      try (PreparedStatement ps = s.getConnection().prepareStatement(
          "INSERT IGNORE INTO subjects(name) VALUES (?)")) {
        ps.setString(1, name);
        ps.executeUpdate();
      }
    }
  }

  private Schema() {
  }
}
