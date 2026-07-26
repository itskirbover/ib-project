package org.example;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ExcelReader {

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
