package org.example;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {

  public static void writeSchedule(File file, List<List<String>> data) throws IOException {
    try (Workbook workbook = new XSSFWorkbook()) {
      Sheet sheet = workbook.createSheet("Schedule");
      for (int r = 0; r < data.size(); r++) {
        Row row = sheet.createRow(r);
        List<String> rowData = data.get(r);
        for (int c = 0; c < rowData.size(); c++) {
          row.createCell(c).setCellValue(rowData.get(c) == null ? "" : rowData.get(c));
        }
      }
      try (FileOutputStream out = new FileOutputStream(file)) {
        workbook.write(out);
      }
    }
  }

  public static List<List<String>> readExcel(File file) throws IOException {
    return readExcelSheet(file, 0);
  }

  public static List<List<String>> readExcelSheet(File file, int sheetIndex) throws IOException {
    List<List<String>> data = new ArrayList<>();
    try (Workbook workbook = WorkbookFactory.create(file)) {
      if (sheetIndex < 0 || sheetIndex >= workbook.getNumberOfSheets()) {
        throw new IllegalArgumentException("Sheet index " + sheetIndex + " out of bounds");
      }
      Sheet sheet = workbook.getSheetAt(sheetIndex);
      DataFormatter formatter = new DataFormatter();

      int maxNumCols = 0;
      for (Row row : sheet) {
        if (row.getLastCellNum() > maxNumCols) {
          maxNumCols = row.getLastCellNum();
        }
      }

      for (Row row : sheet) {
        List<String> rowData = new ArrayList<>();
        for (int i = 0; i < maxNumCols; i++) {
          Cell cell = row.getCell(i, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);
          rowData.add(formatter.formatCellValue(cell));
        }
        data.add(rowData);
      }
    }
    return data;
  }
}
