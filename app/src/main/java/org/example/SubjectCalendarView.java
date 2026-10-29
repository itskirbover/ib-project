package org.example;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/** A calendar showing only one subject's scheduled classes. */
public class SubjectCalendarView extends JPanel {

  public static JComponent create(String subjectName) {
    JPanel host = new JPanel(new BorderLayout());
    host.setOpaque(false);
    List<Slot> slots = Views.slotsOfSubject(subjectName);
    CalendarGrid grid = new CalendarGrid(slots, List.of(), Views.conflicts,
        (day, period, inCell, src, rightClick) -> {
      if (!inCell.isEmpty()) {
        ClassView.open(inCell.get(0));
      }
        });
    JScrollPane scroll = Palette.scrolled(grid);
    host.add(scroll, BorderLayout.CENTER);
    return host;
  }
}
