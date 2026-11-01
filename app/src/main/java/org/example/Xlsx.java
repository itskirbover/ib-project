package org.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

/**
 * Minimal OOXML (.xlsx) reader/writer built only on the JDK: ZipFile +
 * XMLStreamReader. Handles workbooks written by Excel itself (shared
 * strings, multiple sheets, missing cells) as well as files produced by
 * {@link #write}.
 */
final class Xlsx {

  static final String MAIN_NS =
      "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
  static final String REL_NS =
      "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
  static final String PKG_REL_NS =
      "http://schemas.openxmlformats.org/package/2006/relationships";
  static final String CT_NS =
      "http://schemas.openxmlformats.org/package/2006/content-types";

  private Xlsx() {
  }

  // ---------------------------------------------------------------- writing

  /** Writes an xlsx with one sheet per entry (ordered). */
  static void write(File file, LinkedHashMap<String, List<List<String>>> sheets)
      throws IOException {
    try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
      put(zip, "[Content_Types].xml", contentTypes(sheets.size()));
      put(zip, "_rels/.rels", rootRels());
      put(zip, "xl/workbook.xml", workbookXml(sheets));
      put(zip, "xl/_rels/workbook.xml.rels", workbookRels(sheets.size()));
      int i = 0;
      for (List<List<String>> rows : sheets.values()) {
        put(zip, "xl/worksheets/sheet" + (++i) + ".xml", sheetXml(rows));
      }
    }
  }

  private static void put(ZipOutputStream zip, String path, String content)
      throws IOException {
    zip.putNextEntry(new ZipEntry(path));
    zip.write(content.getBytes(StandardCharsets.UTF_8));
    zip.closeEntry();
  }

  private static String contentTypes(int sheetCount) {
    StringBuilder sb = new StringBuilder(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Types xmlns=\"" + CT_NS + "\">"
        + "<Default Extension=\"rels\""
        + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
        + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
        + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd"
        + ".openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>");
    for (int i = 1; i <= sheetCount; i++) {
      sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
          .append("\" ContentType=\"application/vnd.openxmlformats-officedocument"
              + ".spreadsheetml.worksheet+xml\"/>");
    }
    return sb.append("</Types>").toString();
  }

  private static String rootRels() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Relationships xmlns=\"" + PKG_REL_NS + "\">"
        + "<Relationship Id=\"rId1\" Type=\"" + REL_NS + "/officeDocument\""
        + " Target=\"xl/workbook.xml\"/>"
        + "</Relationships>";
  }

  private static String workbookXml(
      LinkedHashMap<String, List<List<String>>> sheets) {
    StringBuilder sb = new StringBuilder(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<workbook xmlns=\"" + MAIN_NS + "\" xmlns:r=\"" + REL_NS
        + "\"><sheets>");
    int i = 0;
    for (String name : sheets.keySet()) {
      sb.append("<sheet name=\"").append(esc(name))
          .append("\" sheetId=\"").append(++i)
          .append("\" r:id=\"rId").append(i).append("\"/>");
    }
    return sb.append("</sheets></workbook>").toString();
  }

  private static String workbookRels(int sheetCount) {
    StringBuilder sb = new StringBuilder(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<Relationships xmlns=\"" + PKG_REL_NS + "\">");
    for (int i = 1; i <= sheetCount; i++) {
      sb.append("<Relationship Id=\"rId").append(i)
          .append("\" Type=\"").append(REL_NS).append("/worksheet\"")
          .append(" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
    }
    return sb.append("</Relationships>").toString();
  }

  private static String sheetXml(List<List<String>> rows) {
    StringBuilder sb = new StringBuilder(
        "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<worksheet xmlns=\"" + MAIN_NS + "\"><sheetData>");
    for (int r = 0; r < rows.size(); r++) {
      List<String> rowData = rows.get(r);
      sb.append("<row r=\"").append(r + 1).append("\">");
      for (int c = 0; c < rowData.size(); c++) {
        String value = rowData.get(c);
        if (value == null || value.isEmpty()) {
          continue;
        }
        sb.append("<c r=\"").append(cellRef(r, c)).append("\" t=\"inlineStr\">")
            .append("<is><t xml:space=\"preserve\">").append(esc(value))
            .append("</t></is></c>");
      }
      sb.append("</row>");
    }
    return sb.append("</sheetData></worksheet>").toString();
  }

  static String cellRef(int row, int col) {
    return colName(col) + (row + 1);
  }

  static String colName(int index) {
    StringBuilder sb = new StringBuilder();
    for (int n = index; n >= 0; n = n / 26 - 1) {
      sb.insert(0, (char) ('A' + n % 26));
    }
    return sb.toString();
  }

  static String esc(String s) {
    StringBuilder sb = new StringBuilder(s.length() + 16);
    for (int i = 0; i < s.length(); i++) {
      char ch = s.charAt(i);
      switch (ch) {
        case '&' -> sb.append("&amp;");
        case '<' -> sb.append("&lt;");
        case '>' -> sb.append("&gt;");
        case '"' -> sb.append("&quot;");
        case '\'' -> sb.append("&apos;");
        default -> {
          if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
            sb.append(ch);
          }
        }
      }
    }
    return sb.toString();
  }

  // ---------------------------------------------------------------- reading

  /** Ordered sheet names with their package part paths. */
  record SheetPart(String name, String path) {
  }

  static List<SheetPart> sheets(ZipFile zip) throws IOException {
    List<SheetPart> result = new ArrayList<>();
    Map<String, String> rels = workbookRels(zip);
    try (InputStream in = stream(zip, "xl/workbook.xml")) {
      XMLStreamReader xml = xmlReader(in);
      while (xml.hasNext()) {
        xml.next();
        if (xml.isStartElement() && "sheet".equals(xml.getLocalName())) {
          String name = attribute(xml, "name");
          String relId = attribute(xml, "r:id");
          String target = rels.get(relId);
          if (target == null) {
            // fall back to the conventional part order
            target = "xl/worksheets/sheet" + (result.size() + 1) + ".xml";
          } else if (target.startsWith("/")) {
            target = target.substring(1);
          } else {
            target = "xl/" + target;
          }
          int idx = result.size() + 1;
          result.add(new SheetPart(name == null ? ("Sheet" + idx) : name,
              target));
        }
      }
    } catch (XMLStreamException e) {
      throw new IOException("Cannot parse workbook.xml", e);
    }
    return result;
  }

  private static Map<String, String> workbookRels(ZipFile zip)
      throws IOException {
    Map<String, String> rels = new HashMap<>();
    String path = "xl/_rels/workbook.xml.rels";
    if (zip.getEntry(path) == null) {
      return rels;
    }
    try (InputStream in = stream(zip, path)) {
      XMLStreamReader xml = xmlReader(in);
      while (xml.hasNext()) {
        xml.next();
        if (xml.isStartElement() && "Relationship".equals(xml.getLocalName())) {
          String id = attribute(xml, "Id");
          String target = attribute(xml, "Target");
          if (id != null) {
            rels.put(id, target == null ? "" : target);
          }
        }
      }
    } catch (XMLStreamException e) {
      throw new IOException("Cannot parse workbook relationships", e);
    }
    return rels;
  }

  private static Map<Integer, String> sharedStrings(ZipFile zip)
      throws IOException {
    Map<Integer, String> shared = new HashMap<>();
    String path = "xl/sharedStrings.xml";
    if (zip.getEntry(path) == null) {
      return shared;
    }
    try (InputStream in = stream(zip, path)) {
      XMLStreamReader xml = xmlReader(in);
      List<String> stack = new ArrayList<>();
      StringBuilder text = new StringBuilder();
      boolean keepText = true;
      int index = 0;
      boolean inItem = false;
      while (xml.hasNext()) {
        int event = xml.next();
        if (event == XMLEvent.START_ELEMENT) {
          String name = xml.getLocalName();
          stack.add(name);
          if ("si".equals(name)) {
            inItem = true;
            text.setLength(0);
          } else if ("rPh".equals(name)) {
            keepText = false;
          }
        } else if (event == XMLEvent.END_ELEMENT) {
          String name = xml.getLocalName();
          stack.remove(stack.size() - 1);
          if ("si".equals(name)) {
            shared.put(index++, text.toString());
            inItem = false;
          } else if ("rPh".equals(name)) {
            keepText = true;
          }
        } else if (event == XMLEvent.CHARACTERS && inItem && keepText
            && !stack.isEmpty() && "t".equals(stack.get(stack.size() - 1))) {
          text.append(xml.getText());
        }
      }
    } catch (XMLStreamException e) {
      throw new IOException("Cannot parse shared strings", e);
    }
    return shared;
  }

  static List<List<String>> readSheet(ZipFile zip, String part)
      throws IOException {
    Map<Integer, String> shared = sharedStrings(zip);
    TreeMap<Integer, List<String>> rowsOut = new TreeMap<>();
    List<String> stack = new ArrayList<>();
    try (InputStream in = stream(zip, part)) {
      XMLStreamReader xml = xmlReader(in);
      int rowIndex = 0;
      int cellIndex = 0;
      List<String> currentRow = null;
      StringBuilder vBuf = new StringBuilder();
      StringBuilder tBuf = new StringBuilder();
      String cellType = "n";
      boolean inCell = false;
      while (xml.hasNext()) {
        int event = xml.next();
        if (event == XMLEvent.START_ELEMENT) {
          String name = xml.getLocalName();
          stack.add(name);
          if ("row".equals(name)) {
            String attr = attribute(xml, "r");
            rowIndex = attr != null ? parseIndex(attr)
                : (rowsOut.isEmpty() ? 0 : rowsOut.lastKey() + 1);
            currentRow = new ArrayList<>();
            cellIndex = 0;
          } else if ("c".equals(name)) {
            inCell = true;
            String attr = attribute(xml, "r");
            cellIndex = attr != null ? colIndexOf(refCol(attr))
                : (currentRow == null || currentRow.isEmpty()
                    ? 0 : cellIndex + 1);
            String t = attribute(xml, "t");
            cellType = t == null || t.isEmpty() ? "n" : t;
            vBuf.setLength(0);
            tBuf.setLength(0);
          }
        } else if (event == XMLEvent.END_ELEMENT) {
          String name = xml.getLocalName();
          stack.remove(stack.size() - 1);
          if ("row".equals(name)) {
            if (currentRow != null) {
              rowsOut.put(rowIndex, currentRow);
            }
            currentRow = null;
          } else if ("c".equals(name) && currentRow != null) {
            currentRow = set(currentRow, cellIndex,
                resolveValue(cellType, vBuf.toString(), tBuf.toString(),
                    shared));
          }
        } else if (event == XMLEvent.CHARACTERS) {
          if (stack.isEmpty()) {
            continue;
          }
          String top = stack.get(stack.size() - 1);
          if ("v".equals(top)) {
            vBuf.append(xml.getText());
          } else if ("t".equals(top) && inCell) {
            tBuf.append(xml.getText());
          }
        }
      }
    } catch (XMLStreamException e) {
      throw new IOException("Cannot parse worksheet: " + part, e);
    }
    return pad(rowsOut);
  }

  private static String resolveValue(String type, String v, String inline,
      Map<Integer, String> shared) {
    switch (type) {
      case "s": {
        String trimmed = v.trim();
        try {
          return shared.getOrDefault(Integer.parseInt(trimmed), "");
        } catch (NumberFormatException e) {
          return "";
        }
      }
      case "inlineStr":
        return inline;
      case "str":
      case "e":
        return v;
      case "b":
        return "1".equals(v.trim()) ? "TRUE" : "FALSE";
      default:
        return formatNumber(v.trim());
    }
  }

  static String formatNumber(String raw) {
    if (raw.isEmpty()) {
      return "";
    }
    try {
      double d = Double.parseDouble(raw);
      if (!Double.isInfinite(d) && !Double.isNaN(d) && d == Math.rint(d)
          && Math.abs(d) <= 1_000_000_000_000.0) {
        return Long.toString((long) d);
      }
      return new BigDecimal(raw).stripTrailingZeros().toPlainString();
    } catch (NumberFormatException e) {
      return raw;
    }
  }

  private static List<String> set(List<String> row, int col, String value) {
    while (row.size() <= col) {
      row.add("");
    }
    row.set(col, value);
    return row;
  }

  private static List<List<String>> pad(
      TreeMap<Integer, List<String>> rowsOut) {
    int maxCols = 0;
    for (List<String> row : rowsOut.values()) {
      maxCols = Math.max(maxCols, row.size());
    }
    List<List<String>> data = new ArrayList<>();
    int expected = rowsOut.isEmpty() ? 0 : rowsOut.lastKey() + 1;
    List<String> empty = new ArrayList<>();
    while (empty.size() < maxCols) {
      empty.add("");
    }
    for (int r = 0; r < expected; r++) {
      List<String> row = rowsOut.getOrDefault(r, empty);
      while (row.size() < maxCols) {
        row.add("");
      }
      data.add(row);
    }
    return data;
  }

  /** ref = e.g. "C5" -> "C". */
  private static String refCol(String ref) {
    int i = 0;
    while (i < ref.length() && (ref.charAt(i) < '0' || ref.charAt(i) > '9')) {
      i++;
    }
    return ref.substring(0, i);
  }

  static int colIndexOf(String letters) {
    int col = 0;
    for (int i = 0; i < letters.length(); i++) {
      col = col * 26 + (letters.charAt(i) - 'A' + 1);
    }
    return col - 1;
  }

  private static int parseIndex(String s) {
    try {
      return Integer.parseInt(s.trim()) - 1;
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  private static String attribute(XMLStreamReader xml, String localName) {
    for (int i = 0; i < xml.getAttributeCount(); i++) {
      if (localName.equals(xml.getAttributeLocalName(i))
          || localName.equals(xml.getAttributeName(i).toString())) {
        return xml.getAttributeValue(i);
      }
    }
    return null;
  }

  private static XMLStreamReader xmlReader(InputStream in)
      throws XMLStreamException {
    XMLInputFactory factory = XMLInputFactory.newFactory();
    factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
    factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
    return factory.createXMLStreamReader(in, "UTF-8");
  }

  private static InputStream stream(ZipFile zip, String path)
      throws IOException {
    ZipEntry entry = zip.getEntry(path);
    if (entry == null) {
      throw new IOException("Not a valid Excel file (missing " + path + ")");
    }
    return zip.getInputStream(entry);
  }
}
