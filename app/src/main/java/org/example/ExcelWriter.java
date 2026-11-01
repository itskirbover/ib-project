package org.example;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Exports a member's (student's or teacher's) personal schedule to Excel:
 * a Period | Time | Mon-Fri grid with one line per class in each cell.
 */
public final class ExcelWriter {

  public static void export(File file, List<Slot> slots, String title)
      throws Exception {
    List<List<String>> rows = new java.util.ArrayList<>();
    rows.add(List.of("Period", "Time", "Mon", "Tue", "Wed", "Thu", "Fri"));

    for (int period = 0; period < Palette.PERIODS; period++) {
      String[] row = new String[7];
      row[0] = "P" + (period + 1);
      row[1] = Palette.periodTime(period);
      for (int day = 0; day < Palette.DAYS; day++) {
        String text = cellText(slots, day, period);
        row[day + 2] = text.isEmpty() ? "" : text;
      }
      rows.add(List.of(row));
    }

    LinkedHashMap<String, List<List<String>>> sheets = new LinkedHashMap<>();
    sheets.put("Timetable", rows);
    Xlsx.write(file, sheets);
  }

  /** "Subject (Teacher)\nGrade · Room" per occupied cell; never empty string only. */
  private static String cellText(List<Slot> slots, int day, int period) {
    StringBuilder text = new StringBuilder();
    for (Slot slot : slots) {
      if (slot.day() != day || slot.period() != period) {
        continue;
      }
      if (text.length() > 0) {
        text.append("\n");
      }
      text.append(slot.subject());
      if (!slot.teacher().isEmpty() && !slot.teacher().equals(TimetableDao.UNASSIGNED)) {
        text.append(" (").append(slot.teacher()).append(")");
      }
      if (!slot.grade().isEmpty() || !slot.room().isEmpty()) {
        text.append("\n");
        if (!slot.grade().isEmpty()) {
          text.append(slot.grade());
        }
        if (!slot.room().isEmpty()) {
          text.append(text.length() > 0 && text.charAt(text.length() - 1) != '\n' ? " · " : "");
          text.append(slot.room());
        }
      }
    }
    return text.toString();
  }

  private ExcelWriter() {
  }
}
