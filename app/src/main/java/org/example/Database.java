package org.example;

import ch.vorburger.mariadb4j.DB;
import ch.vorburger.mariadb4j.DBConfigurationBuilder;

import java.io.File;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Embedded MariaDB (drop-in MySQL-compatible) runtime. The server binaries ship
 * inside the app jar (mariaDB4j-db-linux64) and are unpacked once into
 * ~/.patron/mariadb-base; the live data directory is ~/.patron/mariadb-data so
 * the schedule persists between runs.
 */
public final class Database {
  public static final String DB_NAME = "patron";
  private static final File BASE_DIR = new File(System.getProperty("user.home"), ".patron/mariadb-base");
  private static final File DATA_DIR = new File(System.getProperty("user.home"), ".patron/mariadb-data");

  private static DB server;
  private static DBConfigurationBuilder config;
  private static boolean started;

  private Database() {
  }

  /** Boots the embedded server (idempotent) and initializes the schema. */
  public static synchronized void start() throws Exception {
    if (started) {
      return;
    }
    config = DBConfigurationBuilder.newBuilder()
        .setPort(freePort())
        .setDataDir(DATA_DIR)
        .setBaseDir(BASE_DIR)
        .setLibDir(BASE_DIR)
        .setDeletingTemporaryBaseAndDataDirsOnShutdown(false)
        .setSecurityDisabled(true);
    server = DB.newEmbeddedDB(config.build());
    server.start();
    server.createDB(DB_NAME);
    started = true;
    try (Connection c = open()) {
      Schema.initialize(c);
    }
  }

  public static synchronized void stop() {
    if (started && server != null) {
      try {
        server.stop();
      } catch (Exception ignored) {
        // best effort on shutdown
      }
      started = false;
    }
  }

  public static boolean isRunning() {
    return started;
  }

  public static int port() {
    return config == null ? -1 : config.getPort();
  }

  /** A plain JDBC connection to the embedded server. */
  public static Connection open() throws SQLException {
    String url = "jdbc:mariadb://127.0.0.1:" + port() + "/" + DB_NAME;
    return DriverManager.getConnection(url, "root", "");
  }

  private static int freePort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (Exception e) {
      throw new IllegalStateException("Could not pick a free port for the database", e);
    }
  }
}
