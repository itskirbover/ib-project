package org.example;

import org.junit.jupiter.api.Test;

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

    List<List<String>> sheet = ExcelReader.readExcel(file);
    List<String> header = sheet.get(0);
    assertEquals("Period", header.get(0));
    assertEquals("Time", header.get(1));
    assertEquals("Fri", header.get(6));

    List<String> p1 = sheet.get(1);
    String biologyCell = p1.get(2);
    assertTrue(biologyCell.startsWith("Biology (Nata)"));
    assertTrue(biologyCell.contains("Grade 10 · 203"));

    List<String> period5Row = sheet.get(5); // period index 4 => row 5
    String mathCell = period5Row.get(6);
    assertTrue(mathCell.startsWith("Math")); // (No teacher) placeholder hidden
    assertTrue(mathCell.contains("Grade 11 · 101"));

    // Empty cells stay empty
    assertEquals("", sheet.get(2).get(2));
  }
}
