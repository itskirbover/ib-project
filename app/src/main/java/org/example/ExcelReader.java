package org.example;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.zip.ZipFile;

public class ExcelReader {

  public static void writeSchedule(File file, List<List<String>> data)
      throws IOException {
    LinkedHashMap<String, List<List<String>>> sheets = new LinkedHashMap<>();
    sheets.put("Schedule", data);
    Xlsx.write(file, sheets);
  }

  public static List<List<String>> readExcel(File file) throws IOException {
    return readExcelSheet(file, 0);
  }

  public static List<List<String>> readExcelSheet(File file, int sheetIndex)
      throws IOException {
    try (ZipFile zip = new ZipFile(file)) {
      List<Xlsx.SheetPart> sheets = Xlsx.sheets(zip);
      if (sheetIndex < 0 || sheetIndex >= sheets.size()) {
        throw new IllegalArgumentException(
            "Sheet index " + sheetIndex + " out of bounds");
      }
      return Xlsx.readSheet(zip, sheets.get(sheetIndex).path());
    }
  }
}
