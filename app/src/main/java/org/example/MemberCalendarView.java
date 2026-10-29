package org.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
 * Personal calendar for a student (their subjects' classes) or a teacher
 * (classes they teach). Left-click on an empty slot adds one of their classes;
 * right-click opens the slot menu; teachers toggle Unavailable markings from
 * that menu; the toolbar exports the schedule to Excel.
 */
public class MemberCalendarView extends JPanel {

  public static JComponent create(String memberName) {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);

    Student student = Views.studentByName(memberName);
    Teacher teacher = student == null ? Views.teacherByName(memberName) : null;
    List<Slot> slots = student != null ? Views.slotsOfStudent(student)
        : teacher != null ? Views.slotsOfTeacher(teacher.getName()) : List.of();
    List<int[]> unavailable = teacher != null ? teacher.getBlockedSlots() : List.of();

    CalendarGrid grid = new CalendarGrid(slots, unavailable, Views.conflicts,
        (day, period, inCell, source, rightClick) -> cellClicked(teacher, student, day, period, inCell, source,
            rightClick));

    JPanel toolbar = new JPanel(new BorderLayout());
    toolbar.setOpaque(false);
    toolbar.add(Box.createHorizontalGlue(), BorderLayout.WEST);
    JPanel right = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 8, 8));
    right.setOpaque(false);
    JButton exportBtn = new javax.swing.JButton("Export to Excel");
    exportBtn.addActionListener(e -> exportSchedule(memberName, slots,
        (Component) e.getSource()));
    right.add(exportBtn);
    toolbar.add(right, BorderLayout.EAST);

    JScrollPane scroll = new JScrollPane(grid);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getViewport().setOpaque(false);
    scroll.setOpaque(false);
    host.add(toolbar, BorderLayout.NORTH);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  /* ---- slot interactions ---- */

  private static void cellClicked(Teacher teacher, Student student, int day, int period,
      List<Slot> inCell, Component source, boolean rightClick) {
    if (rightClick) {
      slotMenu(teacher, day, period, inCell, source).show(source, 0, 0);
      return;
    }
    if (!inCell.isEmpty()) {
      ClassView.open(inCell.get(0));
      return;
    }
    if (teacher != null) {
      addClassForTeacher(teacher, day, period, source);
    } else if (student != null) {
      addClassForStudent(student, day, period, source);
    }
  }

  /** Right-click menu: class actions for everyone, Unavailable toggle for teachers. */
  private static JPopupMenu slotMenu(Teacher teacher, int day, int period, List<Slot> inCell,
      Component source) {
    JPopupMenu menu = new JPopupMenu();
    if (teacher != null) {
      boolean blocked = teacher.getBlockedSlots().stream().anyMatch(b -> b[0] == day && b[1] == period);
      JMenuItem toggle = new JMenuItem(blocked ? "Remove unavailable marking" : "Mark slot as unavailable");
      toggle.addActionListener(e -> toggleUnavailable(teacher, day, period));
      menu.add(toggle);
      if (!inCell.isEmpty()) {
        menu.addSeparator();
      }
    }
    for (Slot slot : inCell) {
      JMenuItem open = new JMenuItem(slot.subject() + " (" + slot.teacher() + ") — details");
      open.addActionListener(e -> ClassView.open(slot));
      menu.add(open);
    }
    if (TimetableView.cellHasConflicts(inCell)) {
      JMenuItem solve = new JMenuItem("Solve conflicts");
      solve.addActionListener(e -> TimetableView.solveConflicts(day, period, source));
      menu.add(solve);
    }
    if (!inCell.isEmpty()) {
      menu.addSeparator();
    }
    JMenuItem add = new JMenuItem("Add class here...");
    add.addActionListener(e -> {
      if (teacher != null) {
        addClassForTeacher(teacher, day, period, source);
      }
    });
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

  private static void toggleUnavailable(Teacher teacher, int day, int period) {
    try {
      boolean blocked = teacher.getBlockedSlots().stream().anyMatch(b -> b[0] == day && b[1] == period);
      if (blocked) {
        BlocksDao.remove(teacher.getId(), day, period);
        teacher.getBlockedSlots().removeIf(b -> b[0] == day && b[1] == period);
      } else {
        BlocksDao.add(teacher.getId(), day, period);
        teacher.getBlockedSlots().add(new int[] { day, period });
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    Views.refresh();
  }

  /* ---- add-class dialogs scoped to one member ---- */

  private static void addClassForTeacher(Teacher teacher, int day, int period, Component source) {
    JComboBox<String> subjectBox = AaCombo.comboBox(
        teacher.getCourses().stream().map(s -> s.getName()).toList(), "");
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), "");
    JTextField roomField = new JTextField("", 12);

    JPanel panel = form(subjectBox, gradeBox, roomField,
        Palette.label("Their class will be taught by " + teacher.getName(), 11, null, java.awt.Font.PLAIN));
    Component top = parent(source);
    if (!confirm(top, panel, "Add class — " + Palette.periodTime(period))) {
      return;
    }
    String subject = AaCombo.boxValue(subjectBox);
    if (subject.isBlank()) {
      nothingSelected(top);
      return;
    }
    saveClass(day, period, subject, teacher.getName(), AaCombo.boxValue(gradeBox),
        roomField.getText().trim(), top);
  }

  private static void addClassForStudent(Student student, int day, int period, Component source) {
    JComboBox<String> subjectBox = AaCombo.comboBox(
        student.getCourses().stream().map(s -> s.getName()).toList(), "");
    JComboBox<String> teacherBox = AaCombo.comboBox(List.of(), "");
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), "");
    JTextField roomField = new JTextField("", 12);

    javax.swing.JLabel note = new javax.swing.JLabel();
    note.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 10));
    note.setAlignmentX(Component.LEFT_ALIGNMENT);
    JPanel teacherPart = new JPanel();
    teacherPart.setLayout(new BoxLayout(teacherPart, BoxLayout.Y_AXIS));
    teacherPart.add(AaCombo.labeled(teacherBox, "Teacher"));
    teacherPart.add(note);
    syncTeacherChoices(subjectBox, teacherBox, note);

    JPanel panel = form2(subjectBox, gradeBox, roomField, teacherPart);
    Component top = parent(source);
    if (!confirm(top, panel, "Add class — " + Palette.periodTime(period))) {
      return;
    }
    String subject = AaCombo.boxValue(subjectBox);
    if (subject.isBlank()) {
      nothingSelected(top);
      return;
    }
    saveClass(day, period, subject, AaCombo.boxValue(teacherBox), AaCombo.boxValue(gradeBox),
        roomField.getText().trim(), top);
  }

  /** Teacher dropdown follows the chosen subject: teachers who teach it. */
  private static void syncTeacherChoices(JComboBox<String> subjectBox, JComboBox<String> teacherBox,
      javax.swing.JLabel note) {
    Runnable refill = () -> {
      String subject = AaCombo.boxValue(subjectBox);
      List<String> options = Views.teachers.stream()
          .filter(t -> t.getCourses().stream().anyMatch(sub -> sub.getName().equals(subject)))
          .map(t -> t.getName())
          .toList();
      teacherBox.removeAllItems();
      for (String option : options) {
        teacherBox.addItem(option);
      }
      note.setText(subject.isBlank() || !options.isEmpty() ? " "
          : "No teacher teaches this subject yet — you can still save it.");
      note.revalidate();
    };
    subjectBox.addActionListener((ActionEvent e) -> refill.run());
    refill.run();
  }

  private static JPanel form2(JComboBox<String> subjectBox, JComboBox<String> gradeBox,
      JTextField roomField, JComponent teacherPart) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(AaCombo.labeled(subjectBox, "Subject"));
    panel.add(teacherPart);
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(AaCombo.labeled(roomField, "Room"));
    return panel;
  }

  private static JPanel form(JComboBox<String> subjectBox, JComboBox<String> gradeBox,
      JTextField roomField, JComponent teacherPart) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.add(AaCombo.labeled(subjectBox, "Subject"));
    panel.add(teacherPart);
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(AaCombo.labeled(roomField, "Room"));
    return panel;
  }

  private static boolean confirm(Component top, JComponent panel, String title) {
    return JOptionPane.showConfirmDialog(top, panel, title, JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
  }

  private static void nothingSelected(Component top) {
    JOptionPane.showMessageDialog(top, "Please pick a subject.", "Missing data",
        JOptionPane.WARNING_MESSAGE);
  }

  private static void saveClass(int day, int period, String subject, String teacher,
      String grade, String room, Component top) {
    try {
      SchoolClass klass = new SchoolClass(new Subject(subject), new Teacher(teacher));
      klass.setGrade(grade);
      klass.setRoom(room);
      TimetableDao.save(day, period, klass);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(top, "Failed to save class: " + ex.getMessage(), "Save Error",
          JOptionPane.ERROR_MESSAGE);
      return;
    }
    Views.refresh();
  }

  /* ---- helpers ---- */

  private static Component parent(Component source) {
    return source == null ? null : javax.swing.SwingUtilities.getRoot(source);
  }

  private static void exportSchedule(String memberName, List<Slot> slots, Component source) {
    javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
    chooser.setDialogTitle("Export " + memberName + "'s schedule");
    chooser.setSelectedFile(new java.io.File(memberName.replaceAll("[^\\w\\- ]", "_") + ".xlsx"));
    if (chooser.showSaveDialog(source) != javax.swing.JFileChooser.APPROVE_OPTION) {
      return;
    }
    java.io.File file = chooser.getSelectedFile();
    if (!file.getName().toLowerCase().endsWith(".xlsx")) {
      file = new java.io.File(file.getParentFile(), file.getName() + ".xlsx");
    }
    try {
      ExcelWriter.export(file, slots, memberName);
      JOptionPane.showMessageDialog(source, "Schedule exported to " + file.getAbsolutePath(),
          "Export complete", JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(source, "Export failed: " + ex.getMessage(), "Export Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
