package org.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/**
 * The members menu: two tables (students, teachers) with add and edit forms.
 * Adding/editing persists through the DAOs.
 */
public class MembersView extends JPanel {

  public static JComponent create() {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);

    JPanel column = new JPanel();
    column.setOpaque(false);
    column.setLayout(new javax.swing.BoxLayout(column, javax.swing.BoxLayout.Y_AXIS));
    column.setBorder(BorderFactory.createEmptyBorder(10, 14, 12, 14));

    JButton addStudent = new JButton("+ Add student");
    addStudent.addActionListener(e -> studentDialog(null, host));
    JButton addTeacher = new JButton("+ Add teacher");
    addTeacher.addActionListener(e -> teacherDialog(null, host));

    JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 0));
    buttons.setOpaque(false);
    buttons.add(addStudent);
    buttons.add(addTeacher);
    column.add(buttons);
    column.add(javax.swing.Box.createVerticalStrut(10));

    ArrayList<JPanel> sections = new ArrayList<>();
    sections.add(section("Students", Views.students.stream()
        .sorted(Comparator.comparing(Student::getName))
        .map(s -> (JButton) memberBtn(s.getName() + " (" + s.getGrade() + ")",
            () -> studentDialog(s, host)))
        .toList()));
    sections.add(section("Teachers", Views.teachers.stream()
        .sorted(Comparator.comparing(Teacher::getName))
        .map(t -> (JButton) memberBtn(t.getName() + " — " + t.getCourses().size() + " subjects, "
            + t.countBlockedSlots() + " no-time", () -> teacherDialog(t, host)))
        .toList()));
    JPanel tables = new JPanel(new java.awt.GridLayout(1, 2, 14, 0));
    tables.setOpaque(false);
    for (JPanel sec : sections) {
      tables.add(sec);
    }
    column.add(tables);

    JScrollPane scroll = Palette.scrolled(column);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  private static JButton memberBtn(String label, Runnable action) {
    JButton btn = new JButton(label);
    btn.addActionListener(e -> action.run());
    return btn;
  }

  private static JPanel section(String title, List<JButton> buttons) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    panel.add(Palette.label(title + " (" + buttons.size() + ")", 13,
        Palette.textColors(Palette.windowBackground(panel))[0], Font.BOLD));
    if (buttons.isEmpty()) {
      panel.add(Palette.label("None yet.", 11,
          Palette.textColors(Palette.windowBackground(panel))[3], Font.PLAIN));
    }
    for (JButton btn : buttons) {
      panel.add(btn);
      panel.add(javax.swing.Box.createVerticalStrut(4));
    }
    return panel;
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
    panel.add(javax.swing.Box.createVerticalStrut(4));
    panel.add(Palette.label("Courses (leave blank for a free period):", 11, null, Font.PLAIN));
    for (JComboBox<String> box : courseBoxes) {
      panel.add(box);
    }
    Component top = SwingUtilities.getWindowAncestor(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        existing == null ? "Add student" : "Edit student",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    String name = nameField.getText().trim();
    if (name.isBlank()) {
      Palette.warn(top, "Name is required.", "Missing data");
      return;
    }
    List<org.example.Subject> courses = new ArrayList<>();
    for (JComboBox<String> box : courseBoxes) {
      String value = AaCombo.boxValue(box);
      if (value.isBlank()) {
        continue;
      }
      boolean duplicate = courses.stream().anyMatch(s -> s.getName().equals(value));
      if (!duplicate) {
        org.example.Subject subject = RegistrySubject.lookup(value);
        courses.add(subject);
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
      Palette.error(top, "Failed to save student: " + ex.getMessage(), "Save Error");
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
    panel.add(javax.swing.Box.createVerticalStrut(4));
    panel.add(Palette.label("Courses taught:", 11, null, Font.PLAIN));
    JPanel checks = new JPanel(new java.awt.GridLayout(0, 2, 2, 2));
    checks.setOpaque(false);
    for (JCheckBox check : subjectChecks) {
      checks.add(check);
    }
    panel.add(checks);
    panel.add(javax.swing.Box.createVerticalStrut(4));
    panel.add(Palette.label("No-time slots are set from the teacher's calendar (right-click a slot).",
        10, null, Font.PLAIN));

    Component top = SwingUtilities.getWindowAncestor(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        existing == null ? "Add teacher" : "Edit teacher",
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    String name = nameField.getText().trim();
    if (name.isBlank()) {
      Palette.warn(top, "Name is required.", "Missing data");
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
    List<org.example.Subject> courses = new ArrayList<>();
    for (JCheckBox check : subjectChecks) {
      if (check.isSelected()) {
        courses.add(RegistrySubject.lookup(check.getText()));
      }
    }
    Teacher teacher = new Teacher(name);
    if (existing != null) {
      teacher.setId(existing.getId());
      installBlocksOn(teacher, existing);
    }
    teacher.setMaxHoursPerWeek(maxHours);
    List<int[]> blocks = existing != null ? existing.getBlockedSlots() : new ArrayList<>();
    try {
      TeacherDao.upsert(teacher, courses, blocks);
    } catch (Exception ex) {
      Palette.error(top, "Failed to save teacher: " + ex.getMessage(), "Save Error");
      return;
    }
    Views.refresh();
  }

  private static void installBlocksOn(Teacher target, Teacher from) {
    List<int[]> blocks = new ArrayList<>();
    for (int[] b : from.getBlockedSlots()) {
      blocks.add(new int[] { b[0], b[1] });
    }
    target.installBlocks(blocks);
  }
}

/** Resolves subject rows by name through the DAO. */
final class RegistrySubject {
  static org.example.Subject lookup(String name) {
    try {
      return SubjectDao.findOrCreate(name);
    } catch (Exception e) {
      return new org.example.Subject(name);
    }
  }
}
