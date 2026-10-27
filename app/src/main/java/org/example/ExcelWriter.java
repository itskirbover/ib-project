package org.example;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.List;

/**
 * Exports a member's (student's or teacher's) personal schedule to Excel:
 * a Period | Time | Mon-Fri grid with one line per class in each cell.
 */
public final class ExcelWriter {

  public static void export(java.io.File file, List<Slot> slots, String title) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Timetable");

      Row header = sheet.createRow(0);
      String[] heads = { "Period", "Time", "Mon", "Tue", "Wed", "Thu", "Fri" };
      for (int i = 0; i < heads.length; i++) {
        header.createCell(i).setCellValue(heads[i]);
      }

      for (int period = 0; period < Palette.PERIODS; period++) {
        Row row = sheet.createRow(period + 1);
        row.createCell(0).setCellValue("P" + (period + 1));
        row.createCell(1).setCellValue(Palette.periodTime(period));
        for (int day = 0; day < Palette.DAYS; day++) {
          String text = cellText(slots, day, period);
          if (!text.isEmpty()) {
            row.createCell(day + 2).setCellValue(text);
          }
        }
      }
      for (int i = 0; i < 7; i++) {
        sheet.autoSizeColumn(i);
      }
      try (FileOutputStream out = new FileOutputStream(file)) {
        workbook.write(out);
      }
    }
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
