package org.example;

import java.util.ArrayList;
import java.util.List;

/** A teacher who teaches courses, with weekly hour limits and unavailability. */
public class Teacher extends Member {
  private int maxHoursPerWeek = Integer.MAX_VALUE;
  private final List<int[]> blockedSlots = new ArrayList<>();
  private boolean loadedBlocks = false;

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

  /** No-time slots as [day, period] pairs; loaded from the database on first access. */
  public List<int[]> getBlockedSlots() {
    if (!loadedBlocks && getId() > 0 && Database.isRunning()) {
      try {
        blockedSlots.addAll(BlocksDao.forTeacher(getId()));
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load no-time slots for " + getName(), e);
      }
    }
    loadedBlocks = true;
    return blockedSlots;
  }

  public int countBlockedSlots() {
    return getBlockedSlots().size();
  }

  /** Package-private: DAOs populate blocks from the DB without triggering lazy reload. */
  void installBlocks(List<int[]> blocks) {
    blockedSlots.clear();
    blockedSlots.addAll(blocks);
    loadedBlocks = true;
  }

  /** Period indices (0-based per week) during which this teacher is unavailable on a day. */
  public boolean isAvailable(int day, int period) {
    for (int[] block : getBlockedSlots()) {
      if (block[0] == day && block[1] == period) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String toString() {
    return getName();
  }
}
