package org.example;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Personal calendar for a student (their subjects' classes) or a teacher
 * (classes they teach) — for teachers, every calendar slot can be toggled as
 * a no-time slot via the popup; those render muted red.
 */
public class MemberCalendarView extends JPanel {

  public static JComponent create(String memberName) {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);

    Student student = Views.studentByName(memberName);
    Teacher teacher = student == null ? Views.teacherByName(memberName) : null;
    List<Slot> slots = student != null ? Views.slotsOfStudent(student)
        : teacher != null ? Views.slotsOfTeacher(teacher.getName()) : List.of();
    List<int[]> noTime = teacher != null ? teacher.getBlockedSlots() : List.of();

    CalendarGrid grid = new CalendarGrid(slots, noTime, Views.conflicts,
        (day, period, inCell, src) -> {
          if (!inCell.isEmpty()) {
            Slot slot = inCell.get(0);
            Views.shell.showCard("Class — " + slot.subject(), () -> ClassView.create(slot.id()));
            return;
          }
          if (teacher != null) {
            toggleNoTime(teacher, day, period, src);
          }
        });
    JScrollPane scroll = Palette.scrolled(grid);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }

  /** Empty slot click in a teacher calendar: block/unblock as a no-time slot. */
  private static void toggleNoTime(Teacher teacher, int day, int period, java.awt.Component src) {
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
}
