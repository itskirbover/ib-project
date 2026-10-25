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
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * One scheduled class: subject, teacher, grade, room, static time, the roster
 * of students (everyone whose course list contains this subject), plus any
 * conflicts on this slot in red.
 */
public class ClassView extends JPanel {

  public static JComponent create(int slotId) {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    Slot slot = Views.slots.stream().filter(s -> s.id() == slotId).findFirst().orElse(null);
    if (slot == null) {
      host.add(Palette.label("This class no longer exists.", 14,
          Palette.textColors(Palette.windowBackground(host))[1], Font.PLAIN), BorderLayout.CENTER);
      return host;
    }
    Color bg = Palette.windowBackground(host);
    Color[] texts = Palette.textColors(bg);

    JPanel card = new JPanel();
    card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
    card.setOpaque(false);
    card.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

    card.add(Palette.label(slot.subject(), 22, texts[0], Font.BOLD));
    card.add(Box.createVerticalStrut(2));
    card.add(Palette.label(Palette.dayNames()[slot.day()] + " · " + Palette.periodTime(slot.period()),
        12, texts[1], Font.PLAIN));
    card.add(Box.createVerticalStrut(12));

    card.add(Palette.label("Teacher", 11, texts[2], Font.PLAIN));
    JButton teacherBtn = new JButton("Open calendar");
    teacherBtn.addActionListener(e -> Views.shell.showCard("Teacher — " + slot.teacher(),
        () -> MemberCalendarView.create(slot.teacher())));
    JPanel teacherRow = new JPanel(new BorderLayout());
    teacherRow.setOpaque(false);
    teacherRow.add(Palette.label(slot.teacher(), 14, texts[0], Font.PLAIN), BorderLayout.WEST);
    teacherRow.add(teacherBtn, BorderLayout.EAST);
    card.add(teacherRow);
    card.add(Box.createVerticalStrut(12));

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
    card.add(detailRow);
    card.add(Box.createVerticalStrut(12));

    card.add(Palette.label("Students (" + Views.studentsOfSubject(slot.subject()).size() + ")", 11,
        texts[2], Font.PLAIN));
    List<Student> roster = Views.studentsOfSubject(slot.subject());
    if (roster.isEmpty()) {
      card.add(Palette.label("No students take this subject yet.", 12, texts[3], Font.PLAIN));
    } else {
      for (Student student : roster) {
        JButton studentBtn = new JButton(student.getName() + " — " + student.getGrade());
        studentBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        studentBtn.addActionListener(e -> Views.shell.showCard("Student — " + student.getName(),
            () -> MemberCalendarView.create(student.getName())));
        card.add(studentBtn);
        card.add(Box.createVerticalStrut(3));
      }
    }

    List<String> reasons = Views.conflicts.get(slot.id());
    if (reasons != null && !reasons.isEmpty()) {
      card.add(Box.createVerticalStrut(10));
      Color red = Palette.conflictText(bg);
      for (String reason : reasons) {
        card.add(Palette.label("Conflict: " + reason, 12, red, Font.PLAIN));
      }
    }

    host.add(card, BorderLayout.NORTH);
    return host;
  }

  private static void editDetails(Slot slot, Component source) {
    JPanel panel = new JPanel();
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    JComboBox<String> gradeBox = AaCombo.comboBox(Registry.gradeItems(), slot.grade());
    JTextField roomField = new JTextField(slot.room(), 12);
    panel.add(AaCombo.labeled(gradeBox, "Grade"));
    panel.add(AaCombo.labeled(roomField, "Room"));
    Component top = javax.swing.SwingUtilities.getWindowAncestor(source);
    int result = JOptionPane.showConfirmDialog(top, panel,
        "Edit class details", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }
    try {
      TimetableDao.updateDetails(slot, AaCombo.boxValue(gradeBox), roomField.getText().trim());
    } catch (Exception ex) {
      Palette.error(top, "Failed to update: " + ex.getMessage(), "Save Error");
      return;
    }
    Views.refresh();
  }
}
