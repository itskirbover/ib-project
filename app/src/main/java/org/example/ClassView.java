package org.example;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

/**
 * One timetable slot: every scheduled class in that (day, period) cell as its
 * own section — subject, teacher, grade, room, static time, the roster of
 * students (everyone whose course list contains the subject), any conflicts
 * in red, time-move with a free-time suggestion, and a button replacing the
 * roster with a generated sample set.
 */
public class ClassView extends JPanel {

  /** Convenience: opens the slot view for the cell a class sits in. */
  public static void open(Slot slot) {
    openCell(slot.day(), slot.period());
  }

  /** Convenience: opens the slot view for one (day, period) cell. */
  public static void openCell(int day, int period) {
    Views.shell.showCard(
        "Slot — " + Palette.dayNames()[day] + " · " + Palette.periodTime(period),
        () -> createAt(day, period));
  }

  public static JComponent createAt(int day, int period) {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    List<Slot> inCell = Views.slots.stream()
        .filter(s -> s.day() == day && s.period() == period)
        .toList();
    Color bg = Palette.windowBackground(host);
    Color[] texts = Palette.textColors(bg);

    if (inCell.isEmpty()) {
      host.add(Palette.label("This slot has no classes.", 14, texts[1], Font.PLAIN),
          BorderLayout.CENTER);
      return host;
    }

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setOpaque(false);
    card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

    card.add(Palette.label("Slot " + Palette.dayNames()[day] + " · " + Palette.periodTime(period),
        13, texts[1], Font.BOLD));
    card.add(Box.createVerticalStrut(10));

    for (int i = 0; i < inCell.size(); i++) {
      if (i > 0) {
        card.add(new JSeparator());
        card.add(Box.createVerticalStrut(10));
      }
      card.add(section(inCell.get(i), inCell.size(), texts, bg));
      card.add(Box.createVerticalStrut(12));
    }

    host.add(Palette.scrolled(card), BorderLayout.CENTER);
    return host;
  }

  /** One class's details inside a slot with several parallel classes. */
  private static JComponent section(Slot slot, int parallelCount, Color[] texts, Color bg) {
    JPanel section = new JPanel();
    section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
    section.setOpaque(false);

    section.add(Palette.label(slot.subject(), 20, texts[0], Font.BOLD));
    section.add(Box.createVerticalStrut(2));
    if (parallelCount > 1) {
      section.add(Palette.label("Parallel class " + slot.teacher() + " · "
          + Palette.dayNames()[slot.day()] + " · " + Palette.periodTime(slot.period()),
          12, texts[1], Font.PLAIN));
    } else {
      section.add(Palette.label(Palette.dayNames()[slot.day()] + " · " + Palette.periodTime(slot.period()),
          12, texts[1], Font.PLAIN));
    }
    section.add(Box.createVerticalStrut(10));

    section.add(Palette.label("Teacher", 11, texts[2], Font.PLAIN));
    JButton teacherBtn = new JButton("Open calendar");
    teacherBtn.addActionListener(e -> Views.shell.showCard("Teacher — " + slot.teacher(),
        () -> MemberCalendarView.create(slot.teacher())));
    JPanel teacherRow = new JPanel(new BorderLayout());
    teacherRow.setOpaque(false);
    teacherRow.add(Palette.label(slot.teacher(), 14, texts[0], Font.PLAIN), BorderLayout.WEST);
    teacherRow.add(teacherBtn, BorderLayout.EAST);
    section.add(teacherRow);
    section.add(Box.createVerticalStrut(10));

    JPanel detailRow = new JPanel(new BorderLayout(8, 0));
    detailRow.setOpaque(false);
    JPanel grades = new JPanel();
    grades.setOpaque(false);
    grades.setLayout(new BoxLayout(grades, BoxLayout.Y_AXIS));
    grades.add(Palette.label("Grade", 11, texts[2], Font.PLAIN));
    grades.add(Palette.label(slot.grade().isEmpty() ? "—" : slot.grade(), 14, texts[0], Font.PLAIN));
    JPanel rooms = new JPanel();
    rooms.setOpaque(false);
    rooms.setLayout(new BoxLayout(rooms, BoxLayout.Y_AXIS));
    rooms.add(Palette.label("Room", 11, texts[2], Font.PLAIN));
    rooms.add(Palette.label(slot.room().isEmpty() ? "—" : slot.room(), 14, texts[0], Font.PLAIN));
    detailRow.add(grades, BorderLayout.WEST);
    detailRow.add(rooms, BorderLayout.CENTER);

    JButton editBtn = new JButton("Edit details...");
    editBtn.addActionListener(e -> {
      Component src = (Component) e.getSource();
      editDetails(slot, src);
    });
    detailRow.add(editBtn, BorderLayout.EAST);
    section.add(detailRow);
    section.add(Box.createVerticalStrut(10));

    List<Student> roster = Views.studentsOfSubject(slot.subject());
    section.add(Palette.label("Students (" + roster.size() + ")", 11, texts[2], Font.PLAIN));
    if (roster.isEmpty()) {
      section.add(Palette.label("No students take this subject yet.", 12, texts[3], Font.PLAIN));
    } else {
      for (Student student : roster) {
        JButton studentBtn = new JButton(student.getName() + " — " + student.getGrade());
        studentBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        studentBtn.addActionListener(e -> Views.shell.showCard("Student — " + student.getName(),
            () -> MemberCalendarView.create(student.getName())));
        section.add(studentBtn);
        section.add(Box.createVerticalStrut(3));
      }
    }

    section.add(Box.createVerticalStrut(6));
    JButton sampleBtn = new JButton("Replace students with sample set");
    sampleBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
    sampleBtn.addActionListener(e -> replaceWithSamples(slot, sampleBtn));
    section.add(sampleBtn);

    List<String> reasons = Views.conflicts.get(slot.id());
    if (reasons != null && !reasons.isEmpty()) {
      section.add(Box.createVerticalStrut(10));
      Color red = Palette.conflictText(bg);
      for (String reason : reasons) {
        section.add(Palette.label("Conflict: " + reason, 12, red, Font.PLAIN));
      }
    }
    return section;
  }

  /* ---- edit details: grade, room, and slot move with free-time suggestion ---- */

  private static void editDetails(Slot slot, Component source) {
    Component top = javax.swing.SwingUtilities.getWindowAncestor(source);
    JPanel panel = new JPanel();
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    JComboBox<String> dayBox = AaCombo.comboBox(List.of(Palette.dayNames()),
        Palette.dayNames()[slot.day()]);
    JComboBox<String> periodBox = periodBox(slot.period());
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), slot.grade());
    JTextField roomField = new JTextField(slot.room(), 12);
    panel.add(timeRow(dayBox, periodBox, () -> suggestFreeTime(dayBox, periodBox,
        slot.subject(), slot.teacher(), slot.id(), top)));
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(AaCombo.labeled(roomField, "Room"));
    int result = JOptionPane.showConfirmDialog(top, panel,
        "Edit class details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    int day = dayBox.getSelectedIndex();
    int period = periodBox.getSelectedIndex();
    try {
      TimetableDao.updateDetails(slot, AaCombo.boxValue(gradeBox), roomField.getText().trim());
      if (day >= 0 && period >= 0 && (day != slot.day() || period != slot.period())) {
        TimetableDao.move(slot.id(), day, period);
      }
    } catch (Exception ex) {
      Palette.error(top, moveFailureMessage(ex), "Save Error");
      return;
    }
    Views.refresh();
  }

  /* ---- sample students ---- */

  private static void replaceWithSamples(Slot slot, Component source) {
    Component top = SwingUtilities.getRoot(source);
    int result = JOptionPane.showConfirmDialog(top,
        "Delete the current students of " + slot.subject()
            + " and replace them with a generated sample set (a few with conflicts)?",
        "Replace students", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    try {
      SampleStudents.replaceRoster(slot, 8);
    } catch (Exception ex) {
      ex.printStackTrace();
      Palette.error(top, "Failed to generate sample students: " + ex.getMessage(), "Error");
      return;
    }
    Views.refresh();
  }

  /* ---- shared helpers ---- */

  /** Day + period row for forms, with a button suggesting a conflict-free time. */
  static JComponent timeRow(JComboBox<String> dayBox, JComboBox<String> periodBox,
      Runnable onSuggest) {
    JPanel row = new JPanel(new BorderLayout(8, 0));
    row.setOpaque(false);
    JPanel combos = new JPanel(new BorderLayout(8, 0));
    combos.setOpaque(false);
    combos.add(AaCombo.labeled(dayBox, "Day"), BorderLayout.WEST);
    combos.add(AaCombo.labeled(periodBox, "Period"), BorderLayout.CENTER);
    row.add(combos, BorderLayout.CENTER);
    JButton suggest = new JButton("Suggest free time");
    suggest.addActionListener(e -> onSuggest.run());
    row.add(suggest, BorderLayout.EAST);
    JPanel wrap = new JPanel(new BorderLayout());
    wrap.setOpaque(false);
    wrap.add(row);
    wrap.setAlignmentX(Component.LEFT_ALIGNMENT);
    return wrap;
  }

  static JComboBox<String> periodBox(int initialPeriod) {
    String[] times = new String[Palette.PERIODS];
    for (int i = 0; i < Palette.PERIODS; i++) {
      times[i] = Palette.periodTime(i);
    }
    JComboBox<String> box = AaCombo.comboBox(List.of(times),
        initialPeriod >= 0 ? Palette.periodTime(initialPeriod) : "");
    return box;
  }

  /** Fills the day/period boxes with the first conflict-free slot, if any. */
  static void suggestFreeTime(JComboBox<String> dayBox, JComboBox<String> periodBox,
      String subject, String teacher, int excludeSlotId, Component top) {
    if (subject.isBlank() || teacher.isBlank()) {
      Palette.warn(top, "Pick a subject and a teacher first.", "Nothing to suggest");
      return;
    }
    List<int[]> free = TimeSuggester.findFreeSlots(subject, teacher,
        Views.slots, Views.students, Views.teachers, excludeSlotId);
    if (free.isEmpty()) {
      Palette.warn(top, "No conflict-free slot exists for " + subject + " with " + teacher + ".",
          "No free slot");
      return;
    }
    int[] pick = free.get(0);
    dayBox.setSelectedIndex(pick[0]);
    periodBox.setSelectedItem(Palette.periodTime(pick[1]));
    if (free.size() > 1) {
      showSuggestionNote(top, pick, free.size());
    }
  }

  private static void showSuggestionNote(Component top, int[] pick, int count) {
    Palette.info(top, "Suggested " + Palette.dayNames()[pick[0]] + " · "
        + Palette.periodTime(pick[1]) + " — the first of " + count
        + " conflict-free slots for this class.", "Slot suggestion");
  }

  private static String moveFailureMessage(Exception ex) {
    if (ex instanceof java.sql.SQLException sql
        && sql.getSQLState() != null
        && sql.getSQLState().startsWith("23")) {
      return "That cell already contains this class (same subject and teacher).";
    }
    return "Failed to update: " + ex.getMessage();
  }
}
