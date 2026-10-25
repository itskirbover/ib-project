package org.example;

/**
 * A slot placement used by the calendar views: when and with whom a class runs.
 * Several classes may occupy the same (day, period).
 */
public record Slot(int id, int day, int period, String subject, String teacher, String grade, String room) {

  public String periodLabel() {
    return Palette.periodTime(period);
  }
}
