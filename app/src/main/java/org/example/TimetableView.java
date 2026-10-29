package org.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JComboBox;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;

/** Full timetable: all classes per (day, period). Left-click opens details; right-click the menu. */
public class TimetableView extends JPanel {

  public static JComponent create() {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    CalendarGrid grid = new CalendarGrid(Views.slots, List.of(), Views.conflicts,
        TimetableView::cellClicked);
    JScrollPane scroll = new JScrollPane(grid);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getViewport().setOpaque(false);
    scroll.setOpaque(false);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  private static void cellClicked(int day, int period, List<Slot> inCell, Component source,
      boolean rightClick) {
    if (!inCell.isEmpty() && !rightClick) {
      ClassView.openCell(day, period);
      return;
    }
    if (rightClick) {
      cellMenu(day, period, inCell, source).show(source, 0, 0);
      return;
    }
    addClass(day, period, source);
  }

  /** Right-click menu: open each class, add another parallel class, remove classes. */
  private static JPopupMenu cellMenu(int day, int period, List<Slot> inCell, Component source) {
    JPopupMenu menu = new JPopupMenu();
    for (Slot slot : inCell) {
      JMenuItem open = new JMenuItem(slot.subject() + " (" + slot.teacher() + ") — details");
      open.addActionListener(e -> ClassView.openCell(slot.day(), slot.period()));
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
    return menu;
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

    // Day/period default to the clicked cell, but can be changed or suggested.
    JComboBox<String> dayBox = AaCombo.comboBox(List.of(Palette.dayNames()),
        Palette.dayNames()[day]);
    JComboBox<String> periodBox = ClassView.periodBox(period);
    panel.add(ClassView.timeRow(dayBox, periodBox, () -> ClassView.suggestFreeTime(dayBox,
        periodBox, AaCombo.boxValue(subjectBox), AaCombo.boxValue(teacherBox), 0, source)));

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
      JOptionPane.showMessageDialog(top, "Subject and Teacher are required.", "Missing data",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    int pickedDay = dayBox.getSelectedIndex();
    int pickedPeriod = periodBox.getSelectedIndex();
    if (pickedDay < 0 || pickedPeriod < 0) {
      JOptionPane.showMessageDialog(top, "Day and Period are required.", "Missing data",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    try {
      SchoolClass klass = new SchoolClass(new Subject(subject), new Teacher(teacher));
      klass.setGrade(AaCombo.boxValue(gradeBox));
      klass.setRoom(roomField.getText().trim());
      TimetableDao.save(pickedDay, pickedPeriod, klass);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(top, "Failed to save class: " + ex.getMessage(), "Save Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    Views.refresh();
  }
}
