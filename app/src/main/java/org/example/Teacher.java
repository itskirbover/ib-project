package org.example;

/** A teacher who teaches courses, with weekly hour limits and unavailability. */
public class Teacher extends Member {
  private int maxHoursPerWeek = Integer.MAX_VALUE;

  public Teacher(String name) {
    super(name);
  }

  public int getMaxHoursPerWeek() {
    return maxHoursPerWeek;
  }

  public void setMaxHoursPerWeek(int maxHoursPerWeek) {
    if (maxHoursPerWeek < 0) {
      throw new IllegalArgumentException("Max hours per week must not be negative");
    }
    this.maxHoursPerWeek = maxHoursPerWeek;
  }

  /** Period indices (0-based per week) during which this teacher is unavailable. */
  public boolean isAvailable(int periodIndex) {
    return true;
  }

  @Override
  public String toString() {
    return getName();
  }
}
