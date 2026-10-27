package org.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * The members menu: Teachers and Students sections of visual rows (like the
 * subject menu). Left-click a row opens their calendar; right-click edits;
 * "+ Add" buttons are on top. Editing persists through the DAOs.
 */
public class MembersView extends JPanel {

  public static JComponent create() {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);

    JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(8, 16, 14, 16));

    JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 4));
    buttons.setOpaque(false);
    JButton addStudent = new javax.swing.JButton("+ Add student");
    addStudent.addActionListener(e -> studentDialog(null, host));
    JButton addTeacher = new javax.swing.JButton("+ Add teacher");
    addTeacher.addActionListener(e -> teacherDialog(null, host));
    buttons.add(addStudent);
    buttons.add(addTeacher);
    column.add(buttons);
    column.add(Box.createVerticalStrut(10));

    JPanel sections = new JPanel(new java.awt.GridLayout(1, 2, 16, 0));
    sections.setOpaque(false);
    sections.add(section("Students", Views.students.stream()
        .sorted(Comparator.comparing(Student::getName))
        .map(s -> memberRow(s.getName(), s.getGrade() + " · " + s.getCourses().size() + " course(s)", false))
        .toList()));
    sections.add(section("Teachers", Views.teachers.stream()
        .sorted(Comparator.comparing(Teacher::getName))
        .map(t -> memberRow(t.getName(), t.getCourses().size() + " subject(s) · "
            + t.countBlockedSlots() + " unavailable slots", true))
        .toList()));
    column.add(sections);

    JScrollPane scroll = new JScrollPane(column);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getViewport().setOpaque(false);
    scroll.setOpaque(false);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  /** Opens the edit dialog for a specific member by name. */
  static void edit(String memberName) {
    Student student = Views.studentByName(memberName);
    Teacher teacher = student == null ? Views.teacherByName(memberName) : null;
    if (student != null) {
      studentDialog(student, null);
    } else if (teacher != null) {
      teacherDialog(teacher, null);
    }
  }

  /** Confirms and deletes a teacher or student by name (teacher classes are kept, reassigned). */
  static void remove(String memberName) {
    Student student = Views.studentByName(memberName);
    Teacher teacher = student == null ? Views.teacherByName(memberName) : null;
    String message;
    if (student != null) {
      message = "Remove " + memberName + "? Their course links will be removed.";
    } else if (teacher != null) {
      message = "Remove " + memberName + "? Their classes remain on the timetable as '(No teacher)'.";
    } else {
      return;
    }
    int ok = javax.swing.JOptionPane.showConfirmDialog(null, message,
        "Remove " + memberName, javax.swing.JOptionPane.OK_CANCEL_OPTION,
        javax.swing.JOptionPane.WARNING_MESSAGE);
    if (ok != javax.swing.JOptionPane.OK_OPTION) {
      return;
    }
    try {
      if (student != null) {
        StudentDao.delete(student);
      } else {
        TeacherDao.delete(teacher);
      }
    } catch (Exception ex) {
      ex.printStackTrace();
      Palette.error(null, "Failed to remove: " + ex.getMessage(), "Remove Error");
      return;
    }
    Views.refresh();
  }

  private static JComponent section(String title, List<JComponent> rows) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(Palette.label(title + " (" + rows.size() + ")", 13,
        Palette.textColors(Palette.windowBackground(panel))[0], Font.BOLD));
    panel.add(Box.createVerticalStrut(6));
    if (rows.isEmpty()) {
      panel.add(Palette.label("None yet.", 11,
          Palette.textColors(Palette.windowBackground(panel))[3], Font.PLAIN));
    }
    for (JComponent row : rows) {
      panel.add(row);
      panel.add(Box.createVerticalStrut(5));
    }
    return panel;
  }

  /** Visual member row: left-click calendar, right-click menu (Edit...). */
  private static JComponent memberRow(String memberName, String caption, boolean teacher) {
    JPanel row = new JPanel(new BorderLayout()) {
      @Override
      protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!(g instanceof Graphics2D g2dRaw)) {
          return;
        }
        Graphics2D g2d = (Graphics2D) g2dRaw.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        Color bg = Palette.windowBackground(this);
        Color fill = Palette.cellFill(bg);
        g2d.setColor(fill);
        g2d.fillRoundRect(0, 0, w, h, 14, 14);
        g2d.setColor(Palette.cellBorder(bg, fill));
        g2d.drawRoundRect(0, 0, w, h, 14, 14);
        g2d.dispose();
      }
    };
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
    row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    Color[] texts = Palette.textColors(Palette.windowBackground(row));
    JPanel textSide = new JPanel();
    textSide.setOpaque(false);
    textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));
    textSide.add(Palette.label(memberName, 13, texts[0], Font.PLAIN));
    textSide.add(Palette.label(caption, 10, texts[3], Font.PLAIN));
    row.add(textSide, BorderLayout.CENTER);
    row.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
          JPopupMenu menu = new JPopupMenu();
          JMenuItem edit = new JMenuItem("Edit...");
          edit.addActionListener(ev -> edit(memberName));
          menu.add(edit);
          JMenuItem remove = new JMenuItem(teacher ? "Remove teacher..." : "Remove student...");
          remove.addActionListener(ev -> remove(memberName));
          menu.add(remove);
          menu.show(row, e.getX(), e.getY());
          return;
        }
        Views.shell.showCard("Member — " + memberName, () -> MemberCalendarView.create(memberName));
      }
    });
    return row;
  }

  /* ---- forms ---- */

  private static void studentDialog(Student existing, Component source) {
    JPanel panel = new JPanel();
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    JTextField nameField = new JTextField(existing == null ? "" : existing.getName(), 16);
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), existing == null ? "" : existing.getGrade());
    List<JComboBox<String>> courseBoxes = new ArrayList<>();
    for (int i = 0; i < Student.MAX_COURSES; i++) {
      String initial = existing != null && i < existing.getCourses().size()
          ? existing.getCourses().get(i).getName()
          : "";
      courseBoxes.add(AaCombo.comboBox(Registry.subjectItems(), initial));
    }
    panel.add(AaCombo.labeled(nameField, "Name"));
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(Box.createVerticalStrut(4));
    panel.add(Palette.label("Courses (leave blank for a free period):", 11, null, Font.PLAIN));
    for (JComboBox<String> box : courseBoxes) {
      panel.add(box);
    }
    Component top = source == null ? null : javax.swing.SwingUtilities.getRoot(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        existing == null ? "Add student" : "Edit student — " + existing.getName(),
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    String name = nameField.getText().trim();
    if (name.isBlank()) {
      JOptionPane.showMessageDialog(top, "Name is required.", "Missing data", JOptionPane.WARNING_MESSAGE);
      return;
    }
    List<Subject> courses = new ArrayList<>();
    for (JComboBox<String> box : courseBoxes) {
      String value = AaCombo.boxValue(box);
      if (value.isBlank()) {
        continue;
      }
      boolean duplicate = courses.stream().anyMatch(s -> s.getName().equals(value));
      if (!duplicate) {
        courses.add(RegistrySubject.lookup(value));
      }
    }
    if (courses.size() > Student.MAX_COURSES) {
      JOptionPane.showMessageDialog(top, "A student can take at most " + Student.MAX_COURSES + " courses.",
          "Too many courses", JOptionPane.WARNING_MESSAGE);
      return;
    }
    Student student = new Student(name, AaCombo.boxValue(gradeBox));
    if (existing != null) {
      student.setId(existing.getId());
    }
    try {
      StudentDao.upsert(student, courses);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(top, "Failed to save student: " + ex.getMessage(), "Save Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    Views.refresh();
  }

  private static void teacherDialog(Teacher existing, Component source) {
    JPanel panel = new JPanel();
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    JTextField nameField = new JTextField(existing == null ? "" : existing.getName(), 16);
    JTextField maxHoursField = new JTextField(
        existing == null || existing.getMaxHoursPerWeek() == Integer.MAX_VALUE
            ? ""
            : String.valueOf(existing.getMaxHoursPerWeek()),
        4);
    JCheckBox[] subjectChecks = Registry.subjectItems().stream()
        .filter(s -> !s.startsWith("+"))
        .map(s -> {
          JCheckBox check = new JCheckBox(s);
          if (existing != null && existing.getCourses().stream().anyMatch(sub -> sub.getName().equals(s))) {
            check.setSelected(true);
          }
          return check;
        })
        .toArray(JCheckBox[]::new);
    panel.add(AaCombo.labeled(nameField, "Name"));
    panel.add(AaCombo.labeled(maxHoursField, "Max hours/week"));
    panel.add(Box.createVerticalStrut(4));
    panel.add(Palette.label("Courses taught:", 11, null, Font.PLAIN));
    JPanel checks = new JPanel(new java.awt.GridLayout(0, 2, 2, 2));
    checks.setOpaque(false);
    for (JCheckBox check : subjectChecks) {
      checks.add(check);
    }
    panel.add(checks);
    panel.add(Box.createVerticalStrut(4));
    panel.add(Palette.label("Unavailable slots are set from the teacher's calendar (right-click an empty slot).",
        10, null, Font.PLAIN));

    Component top = source == null ? null : javax.swing.SwingUtilities.getRoot(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        existing == null ? "Add teacher" : "Edit teacher — " + existing.getName(),
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    String name = nameField.getText().trim();
    if (name.isBlank()) {
      JOptionPane.showMessageDialog(top, "Name is required.", "Missing data", JOptionPane.WARNING_MESSAGE);
      return;
    }
    int maxHours = 0;
    try {
      String text = maxHoursField.getText().trim();
      if (!text.isBlank()) {
        maxHours = Integer.parseInt(text);
      }
    } catch (NumberFormatException ex) {
      JOptionPane.showMessageDialog(top, "Max hours must be a number.", "Invalid input",
          JOptionPane.WARNING_MESSAGE);
      return;
    }
    List<Subject> courses = new ArrayList<>();
    for (JCheckBox check : subjectChecks) {
      if (check.isSelected()) {
        courses.add(RegistrySubject.lookup(check.getText()));
      }
    }
    Teacher teacher = new Teacher(name);
    int teacherId = existing == null ? -1 : existing.getId();
    List<int[]> blocks = existing == null ? new ArrayList<>() : existing.getBlockedSlots();
    teacher.setId(teacherId);
    teacher.installBlocks(new ArrayList<>());
    teacher.getBlockedSlots();
    teacher.installBlocks(blocks);
    teacher.setMaxHoursPerWeek(maxHours);
    try {
      TeacherDao.upsert(teacher, courses, blocks);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(top, "Failed to save teacher: " + ex.getMessage(), "Save Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    try {
      for (Subject subject : courses) {
        TimetableDao.reassignUnassigned(subject.getName(), teacher);
      }
    } catch (Exception ex) {
      System.err.println("Auto-assign failed: " + ex.getMessage());
    }
    Views.refresh();
  }
}

/** Resolves subject rows by name through the DAO. */
final class RegistrySubject {
  static Subject lookup(String name) {
    try {
      return SubjectDao.findOrCreate(name);
    } catch (Exception e) {
      return new Subject(name);
    }
  }
}
