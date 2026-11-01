package org.example;

import java.util.Objects;

/** An IB DP course offered by the school. */
public class Subject {
  /** Higher level: 6 periods (3 sessions) per week. */
  public static final String HL = "HL";
  /** Standard level: 4 periods (2 sessions) per week. */
  public static final String SL = "SL";

  private final String name;
  private int id = -1;
  private String level = SL;

  public Subject(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Subject name must not be null");
    }
    this.name = name.trim();
  }

  /** Level on the DP scale: {@link #HL} or {@link #SL}. */
  public String getLevel() {
    return level;
  }

  public void setLevel(String level) {
    this.level = HL.equals(level) ? HL : SL;
  }

  /** Periods per week the DP timetable rules demand (HL 6, SL 4). */
  public int requiredPeriods() {
    return HL.equals(level) ? 6 : 4;
  }

  /** Dual sessions per week (requiredPeriods / 2). */
  public int requiredSessions() {
    return requiredPeriods() / 2;
  }

  /** Database id; -1 until the entity has been loaded from or saved to the DB. */
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Subject other && other.name.equals(name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
