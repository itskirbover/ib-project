package org.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Full timetable: all classes per (day, period), edited via the cell popup. */
public class TimetableView extends JPanel {

  public static JComponent create() {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    CalendarGrid grid = new CalendarGrid(Views.slots, List.of(), Views.conflicts,
        TimetableView::cellClicked);
    JScrollPane scroll = Palette.scrolled(grid);
    host.add(toolbar(grid), BorderLayout.NORTH);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  /** Top bar: import a legacy Excel schedule into the database. */
  private static JComponent toolbar(Component source) {
    JPanel bar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 8));
    bar.setOpaque(false);
    javax.swing.JButton importBtn = new javax.swing.JButton("Import from Excel");
    importBtn.addActionListener(e -> importExcel(SwingUtilities.getAncestorOfClass(java.awt.Window.class,
        (Component) e.getSource())));
    bar.add(importBtn);
    bar.add(javax.swing.Box.createHorizontalStrut(6));
    StatisticsLabel stats = new StatisticsLabel();
    bar.add(stats);
    return bar;
  }

  private static void importExcel(Component parent) {
    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
    chooser.setDialogTitle("Import Schedule Excel File");
    chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Excel Spreadsheets", "xlsx", "xls"));
    if (chooser.showOpenDialog(parent) != javax.swing.JFileChooser.APPROVE_OPTION) {
      return;
    }
    try {
      List<List<String>> rows = ExcelReader.readExcel(chooser.getSelectedFile());
      int imported = 0;
      for (int period = 0; period < Palette.PERIODS; period++) {
        for (int day = 0; day < Palette.DAYS; day++) {
          int index = 1 + period * Palette.DAYS + day;
          if (index >= rows.size()) {
            break;
          }
          List<String> row = rows.get(index);
          String subject = col(row, 0);
          if (subject.isBlank()) {
            continue;
          }
          String grade = col(row, 1);
          String teacher = col(row, 2).isBlank() ? "Unknown Teacher" : col(row, 2);
          String room = col(row, 3);
          Registry.addSubject(subject);
          Registry.addTeacher(teacher);
          SchoolClass klass = new SchoolClass(new Subject(subject), new Teacher(teacher));
          klass.setGrade(grade.isEmpty() ? "" : grade.substring(0, Math.min(grade.length(), 39)));
          klass.setRoom(room.isEmpty() ? "" : room.substring(0, Math.min(room.length(), 39)));
          TimetableDao.deleteSubjectInSlot(day, period, subject);
          TimetableDao.save(day, period, klass);
          imported++;
        }
      }
      Views.refresh();
      JOptionPane.showMessageDialog(parent,
          imported + " classes imported into the timetable.", "Import complete",
          JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      ex.printStackTrace();
      Palette.error(parent, "Import failed: " + ex.getMessage(), "Import Error");
    }
  }

  private static String col(List<String> row, int i) {
    return i < row.size() && row.get(i) != null ? row.get(i).trim() : "";
  }

  private static class StatisticsLabel extends javax.swing.JLabel {
    StatisticsLabel() {
      setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 12));
      setText(classesText());
    }

    @Override
    public void paint(java.awt.Graphics g) {
      setText(classesText());
      super.paint(g);
    }

    private String classesText() {
      StringBuilder text = new StringBuilder("Classes: " + Views.slots.size());
      if (!Views.conflicts.isEmpty()) {
        text.append("  ·  Conflicts: ").append(Views.conflicts.size());
      }
      return text.toString();
    }
  }

  /** Cell menu: open each class, add another parallel class, remove classes. */
  private static void cellClicked(int day, int period, List<Slot> inCell, Component source) {
    JPopupMenu menu = new JPopupMenu();
    for (Slot slot : inCell) {
      JMenuItem open = new JMenuItem(slot.subject() + " (" + slot.teacher() + ") — details");
      open.addActionListener(e -> Views.shell.showCard("Class — " + slot.subject(),
          () -> ClassView.create(slot.id())));
      menu.add(open);
    }
    if (!inCell.isEmpty()) {
      menu.addSeparator();
    }
    JMenuItem add = new JMenuItem("Add class here...");
    add.addActionListener(e -> addClass(day, period, source));
    menu.add(add);
    for (Slot slot : inCell) {
      JMenuItem remove = new JMenuItem("Remove " + slot.subject());
      remove.addActionListener(e -> {
        try {
          TimetableDao.delete(slot.id());
        } catch (Exception ex) {
          ex.printStackTrace();
        }
        Views.refresh();
      });
      menu.add(remove);
    }
    menu.show(source, 0, 0);
  }

  static void addClass(int day, int period, Component source) {
    JPanel panel = new JPanel();
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    JComboBox<String> subjectBox = AaCombo.comboBox(Registry.subjectItems(), "");
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), "");
    JComboBox<String> teacherBox = AaCombo.comboBox(Registry.teacherItems(), "");
    JTextField roomField = new JTextField("", 12);
    AaCombo.attachAddListener(subjectBox, AaCombo.CatalogKind.SUBJECT);
    AaCombo.attachAddListener(teacherBox, AaCombo.CatalogKind.TEACHER);

    panel.add(AaCombo.labeled(subjectBox, "Subject"));
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(AaCombo.labeled(teacherBox, "Teacher"));
    panel.add(AaCombo.labeled(roomField, "Room"));

    Component top = source == null ? null : SwingUtilities.getRoot(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        "Add class — " + Palette.periodTime(period), JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    String subject = AaCombo.boxValue(subjectBox);
    String teacher = AaCombo.boxValue(teacherBox);
    if (subject.isBlank() || teacher.isBlank()) {
      Palette.warn(top, "Subject and Teacher are required.", "Missing data");
      return;
    }
    try {
      SchoolClass klass = new SchoolClass(new Subject(subject), new Teacher(teacher));
      klass.setGrade(AaCombo.boxValue(gradeBox));
      klass.setRoom(roomField.getText().trim());
      TimetableDao.save(day, period, klass);
    } catch (Exception ex) {
      Palette.error(top, "Failed to save class: " + ex.getMessage(), "Save Error");
      return;
    }
    Views.refresh();
  }
}
