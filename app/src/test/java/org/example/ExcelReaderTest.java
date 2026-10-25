package org.example;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderTest {

    @Test
    void testReadExcel(@TempDir Path tempDir) throws IOException {
        // 1. Create a temporary Excel file
        File excelFile = tempDir.resolve("test_schedule.xlsx").toFile();
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("TestSheet");
            
            // Row 0: Headers
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Time");
            headerRow.createCell(1).setCellValue("Subject");
            headerRow.createCell(2).setCellValue("Room");
            
            // Row 1: Data 1
            Row row1 = sheet.createRow(1);
            row1.createCell(0).setCellValue("09:00 AM");
            row1.createCell(1).setCellValue("Mathematics");
            row1.createCell(2).setCellValue(101.0); // Numeric cell
            
            // Row 2: Data 2
            Row row2 = sheet.createRow(2);
            row2.createCell(0).setCellValue("10:30 AM");
            row2.createCell(1).setCellValue("Physics");
            // Cell 2 is left blank intentionally
            
            // Save workbook to file
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }
        
        // 2. Read the file using ExcelReader
        List<List<String>> data = ExcelReader.readExcel(excelFile);
        
        // 3. Verify content
        assertNotNull(data);
        assertEquals(3, data.size());
        
        // Assert header row
        List<String> headers = data.get(0);
        assertEquals(3, headers.size());
        assertEquals("Time", headers.get(0));
        assertEquals("Subject", headers.get(1));
        assertEquals("Room", headers.get(2));
        
        // Assert row 1
        List<String> r1 = data.get(1);
        assertEquals(3, r1.size());
        assertEquals("09:00 AM", r1.get(0));
        assertEquals("Mathematics", r1.get(1));
        assertEquals("101", r1.get(2)); // Numeric 101.0 formatted as string "101"
        
        // Assert row 2
        List<String> r2 = data.get(2);
        assertEquals(3, r2.size());
        assertEquals("10:30 AM", r2.get(0));
        assertEquals("Physics", r2.get(1));
        assertEquals("", r2.get(2)); // Missing cell read as empty string
    }

    @Test
    void testReadInvalidSheetIndex(@TempDir Path tempDir) throws IOException {
        File excelFile = tempDir.resolve("test_empty.xlsx").toFile();
        try (Workbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("OnlySheet");
            try (FileOutputStream fos = new FileOutputStream(excelFile)) {
                workbook.write(fos);
            }
        }

        assertThrows(IllegalArgumentException.class, () -> {
            ExcelReader.readExcelSheet(excelFile, 1); // 1 is out of bounds (only index 0 exists)
        });
    }
}
