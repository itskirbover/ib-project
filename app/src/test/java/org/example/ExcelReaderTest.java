package org.example;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

class ExcelReaderTest {

    @Test
    void testReadExcel(@TempDir Path tempDir) throws IOException {
        // 1. Create a temporary Excel file
        File excelFile = tempDir.resolve("test_schedule.xlsx").toFile();

        // Row 0: Headers
        // Row 1: Data 1
        // Row 2: Data 2 (cell 2 left blank intentionally)
        List<List<String>> data = List.of(
            List.of("Time", "Subject", "Room"),
            List.of("09:00 AM", "Mathematics", "101"),
            List.of("10:30 AM", "Physics", ""));
        ExcelReader.writeSchedule(excelFile, data);

        // 2. Read the file using ExcelReader
        List<List<String>> read = ExcelReader.readExcel(excelFile);

        // 3. Verify content
        assertNotNull(read);
        assertEquals(3, read.size());

        // Assert header row
        List<String> headers = read.get(0);
        assertEquals(3, headers.size());
        assertEquals("Time", headers.get(0));
        assertEquals("Subject", headers.get(1));
        assertEquals("Room", headers.get(2));

        // Assert row 1
        List<String> r1 = read.get(1);
        assertEquals(3, r1.size());
        assertEquals("09:00 AM", r1.get(0));
        assertEquals("Mathematics", r1.get(1));
        assertEquals("101", r1.get(2));

        // Assert row 2
        List<String> r2 = read.get(2);
        assertEquals(3, r2.size());
        assertEquals("10:30 AM", r2.get(0));
        assertEquals("Physics", r2.get(1));
        assertEquals("", r2.get(2)); // Missing cell read as empty string
    }

    /** A workbook hand-crafted the way Excel writes one: shared strings,
     *  numeric cells and two sheets. */
    @Test
    void testReadExcelWorkbookFile(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("excel_made.xlsx").toFile();
        writeWorkbookFixture(file);

        List<List<String>> first = ExcelReader.readExcelSheet(file, 0);
        assertEquals(3, first.size());
        assertEquals("Time", first.get(0).get(0));
        assertEquals("Room", first.get(0).get(2));
        assertEquals("09:00 AM", first.get(1).get(0));
        assertEquals("101", first.get(1).get(2)); // numeric 101.0 -> "101"
        assertEquals("", first.get(2).get(2));    // blank cell
        assertEquals("Physics", first.get(2).get(1));

        // Second sheet exists and carries inline strings + decimal number
        List<List<String>> second = ExcelReader.readExcelSheet(file, 1);
        assertEquals("X", second.get(0).get(0));
        assertEquals("2.5", second.get(1).get(0));
    }

    @Test
    void testReadInvalidSheetIndex(@TempDir Path tempDir) throws IOException {
        File excelFile = tempDir.resolve("one_sheet.xlsx").toFile();
        ExcelReader.writeSchedule(excelFile, List.of(List.of("A")));

        assertThrows(IllegalArgumentException.class, () -> {
            ExcelReader.readExcelSheet(excelFile, 1); // out of bounds (only 0 exists)
        });
    }

    @Test
    void testEscapeAndNewlines(@TempDir Path tempDir) throws IOException {
        File file = tempDir.resolve("escaped.xlsx").toFile();
        ExcelReader.writeSchedule(file, List.of(
            List.of("a & b <c>", "line1\nline2", "\"quoted\"")));
        List<List<String>> read = ExcelReader.readExcel(file);
        assertEquals("a & b <c>", read.get(0).get(0));
        assertEquals("line1\nline2", read.get(0).get(1));
        assertEquals("\"quoted\"", read.get(0).get(2));
    }

    // ------------------------------------------------------------------ fixture

    private static void writeWorkbookFixture(File file) throws IOException {
        try (ZipOutputStream zip =
                 new ZipOutputStream(new FileOutputStream(file))) {
            put(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
                          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>
                    <sheet name="TestSheet" sheetId="1" r:id="rId1"/>
                    <sheet name="Extra" sheetId="2" r:id="rId2"/>
                  </sheets>
                </workbook>""");
            put(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet1.xml"/>
                  <Relationship Id="rId2"
                    Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet"
                    Target="worksheets/sheet2.xml"/>
                </Relationships>""");
            put(zip, "xl/sharedStrings.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <si><t>Time</t></si>
                  <si><t>Subject</t></si>
                  <si><t>Room</t></si>
                  <si><t>09:00 AM</t></si>
                  <si><t>Mathematics</t></si>
                  <si><t>10:30 AM</t></si>
                  <si><t>Physics</t></si>
                </sst>""");
            put(zip, "xl/worksheets/sheet1.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1">
                      <c r="A1" t="s"><v>0</v></c>
                      <c r="B1" t="s"><v>1</v></c>
                      <c r="C1" t="s"><v>2</v></c>
                    </row>
                    <row r="2">
                      <c r="A2" t="s"><v>3</v></c>
                      <c r="B2" t="s"><v>4</v></c>
                      <c r="C2"><v>101.0</v></c>
                    </row>
                    <row r="3">
                      <c r="A3" t="s"><v>5</v></c>
                      <c r="B3" t="s"><v>6</v></c>
                    </row>
                  </sheetData>
                </worksheet>""");
            put(zip, "xl/worksheets/sheet2.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>
                    <row r="1"><c r="A1" t="inlineStr"><is><t xml:space="preserve">X</t></is></c></row>
                    <row r="2"><c r="A2"><v>2.5</v></c></row>
                  </sheetData>
                </worksheet>""");
        }
    }

    private static void put(ZipOutputStream zip, String path, String content)
        throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        OutputStream body = zip;
        body.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
