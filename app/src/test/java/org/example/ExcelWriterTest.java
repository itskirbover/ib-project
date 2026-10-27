package org.example;

import org.junit.jupiter.api.Test;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Export produces a readable xlsx with the member's slots laid out in a grid. */
class ExcelWriterTest {

  @Test
  void exportsOccupiedSlotsWithHeader() throws Exception {
    File file = File.createTempFile("export", ".xlsx");
    file.deleteOnExit();
    Slot mondayP1 = new Slot(1, 0, 0, "Biology", "Nata", "Grade 10", "203");
    Slot fridayP5 = new Slot(2, 4, 4, "Math", "(No teacher)", "Grade 11", "101");
    ExcelWriter.export(file, List.of(mondayP1, fridayP5), "Ana");

    try (XSSFWorkbook workbook = new XSSFWorkbook(file)) {
      var sheet = workbook.getSheetAt(0);
      Row header = sheet.getRow(0);
      assertEquals("Period", header.getCell(0).getStringCellValue());
      assertEquals("Time", header.getCell(1).getStringCellValue());
      assertEquals("Fri", header.getCell(6).getStringCellValue());
      Row p1 = sheet.getRow(1);
      String biologyCell = p1.getCell(2).getStringCellValue();
      assertTrue(biologyCell.startsWith("Biology (Nata)"));
      assertTrue(biologyCell.contains("Grade 10 · 203"));
      Row period5Row = sheet.getRow(5); // period index 4 => row 5
      String mathCell = period5Row.getCell(6).getStringCellValue();
      assertTrue(mathCell.startsWith("Math")); // (No teacher) placeholder hidden
      assertTrue(mathCell.contains("Grade 11 · 101"));
      // Empty cells stay empty
      assertEquals(null, sheet.getRow(2).getCell(2));
    }
  }
}
