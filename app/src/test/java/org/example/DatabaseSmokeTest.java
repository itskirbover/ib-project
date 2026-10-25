package org.example;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Starts the embedded MariaDB for real and checks basic DDL + CRUD. */
class DatabaseSmokeTest {
  @Test
  void startsServerAndRunsCrud() throws Exception {
    Database.start();
    try (Connection c = Database.open()) {
      try (java.sql.Statement s = c.createStatement()) {
        s.executeUpdate("DELETE FROM subjects WHERE name = 'Smoke Subject'");
      }
      try (PreparedStatement ps = c.prepareStatement("INSERT INTO subjects(name) VALUES (?)")) {
        ps.setString(1, "Smoke Subject");
        ps.executeUpdate();
      }
      try (PreparedStatement ps = c.prepareStatement("SELECT id, name FROM subjects WHERE name = ?")) {
        ps.setString(1, "Smoke Subject");
        try (ResultSet rs = ps.executeQuery()) {
          assertTrue(rs.next());
          assertEquals("Smoke Subject", rs.getString("name"));
        }
      }
    }
    Database.stop();
  }
}
