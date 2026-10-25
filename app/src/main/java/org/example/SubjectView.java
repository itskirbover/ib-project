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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Home view: every subject in one list, each with an open/close accordion for
 * its members and scheduled periods. Pressing a subject opens its calendar.
 */
public class SubjectView extends JPanel {
  private static final Set<String> expanded = new HashSet<>();

  public static JComponent create() {
    SubjectView view = new SubjectView();
    view.setLayout(new java.awt.BorderLayout());
    view.setOpaque(false);

    JPanel toolbar = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 8, 10));
    toolbar.setOpaque(false);
    JButton addBtn = new JButton(Registry.ADD_SUBJECT.substring(0, Registry.ADD_SUBJECT.length() - 3));
    addBtn.addActionListener(e -> {
      String name = AaCombo.prompt(view, "Subject name");
      if (name != null && !name.isBlank()) {
        Registry.addSubject(name);
        Views.refresh();
        Views.shell.rerenderCurrent();
      }
    });
    toolbar.add(addBtn);

    JPanel list = new JPanel();
    list.setOpaque(false);
    list.setLayout(new javax.swing.BoxLayout(list, javax.swing.BoxLayout.Y_AXIS));

    try {
      for (org.example.Subject subject : SubjectDao.all()) {
        list.add(row(subject, view));
      }
    } catch (Exception ex) {
      list.add(Palette.label("Database error: " + ex.getMessage(), 12, Color.RED, Font.PLAIN));
    }

    JScrollPane scroll = Palette.scrolled(list);

    view.add(toolbar, BorderLayout.NORTH);
    view.add(scroll, BorderLayout.CENTER);
    return view;
  }

  private static JPanel row(Subject subject, Component parent) {
    String name = subject.getName();
    List<Teacher> teachers = Views.teachers.stream()
        .filter(t -> t.getCourses().stream().anyMatch(s -> s.getName().equals(name)))
        .toList();
    List<Student> students = Views.studentsOfSubject(name);
    List<Slot> slots = Views.slotsOfSubject(name).stream()
        .sorted(Comparator.comparingInt(Slot::day).thenComparingInt(Slot::period))
        .toList();

    JPanel wrap = new JPanel(new java.awt.GridBagLayout());
    wrap.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(4, 8, 4, 8);
    wrap.add(header(name), gbc);

    if (expanded.contains(name)) {
      gbc.gridy++;
      wrap.add(accordion(name, teachers, students, slots, parent), gbc);
    }
    return wrap;
  }

  /** Rounded subject header: click opens the calendar; side button folds/unfolds. */
  private static JPanel header(String name) {
    JPanel panel = new JPanel(new java.awt.BorderLayout()) {
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
    panel.setOpaque(false);
    panel.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 8));
    panel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    Color bg = Palette.windowBackground(panel);
    Color[] texts = Palette.textColors(bg);

    panel.add(Palette.label(name, 14, texts[0], Font.BOLD), java.awt.BorderLayout.CENTER);

    JButton arrow = new JButton(expanded.contains(name) ? "v" : ">");
    arrow.setFont(new Font("Arial", Font.PLAIN, 13));
    arrow.addActionListener(e -> toggle(name));
    panel.add(arrow, java.awt.BorderLayout.EAST);

    panel.addMouseListener(new java.awt.event.MouseAdapter() {
      @Override
      public void mousePressed(java.awt.event.MouseEvent e) {
        Views.shell.showCard("Subject — " + name, () -> SubjectCalendarView.create(name));
      }
    });
    return panel;
  }

  private static void toggle(String name) {
    if (expanded.contains(name)) {
      expanded.remove(name);
    } else {
      expanded.add(name);
    }
    Views.shell.rerenderCurrent();
  }

  /** Expanded part: members and periods. */
  private static JComponent accordion(String name, List<Teacher> teachers, List<Student> students,
      List<Slot> slots, Component parent) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new javax.swing.BoxLayout(panel, javax.swing.BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(2, 22, 6, 10));
    Color bg = Palette.windowBackground(panel);
    Color[] texts = Palette.textColors(bg);

    panel.add(Palette.label("Members", 12, texts[2], Font.BOLD));
    if (teachers.isEmpty() && students.isEmpty()) {
      panel.add(Palette.label("No members enrolled yet.", 11, texts[3], Font.PLAIN));
    }
    for (Teacher teacher : teachers) {
      JButton btn = new JButton("T · " + teacher.getName());
      btn.setAlignmentX(Component.LEFT_ALIGNMENT);
      btn.addActionListener(e -> Views.shell.showCard("Teacher — " + teacher.getName(),
          () -> MemberCalendarView.create(teacher.getName())));
      panel.add(btn);
    }
    for (Student student : students) {
      JButton btn = new JButton("S · " + student.getName() + " (" + student.getGrade() + ")");
      btn.setAlignmentX(Component.LEFT_ALIGNMENT);
      btn.addActionListener(e -> Views.shell.showCard("Student — " + student.getName(),
          () -> MemberCalendarView.create(student.getName())));
      panel.add(btn);
    }
    panel.add(Box.createVerticalStrut(6));

    panel.add(Palette.label("Periods", 12, texts[2], Font.BOLD));
    if (slots.isEmpty()) {
      panel.add(Palette.label("Not scheduled yet.", 11, texts[3], Font.PLAIN));
    }
    for (Slot slot : slots) {
      String label = Palette.dayNames()[slot.day()] + " · " + slot.periodLabel()
          + (slot.grade().isEmpty() ? "" : " · " + slot.grade())
          + (slot.room().isEmpty() ? "" : " · " + slot.room());
      JButton btn = new JButton(label);
      btn.setAlignmentX(Component.LEFT_ALIGNMENT);
      btn.addActionListener(e -> Views.shell.showCard("Class — " + slot.subject(),
          () -> ClassView.create(slot.id())));
      panel.add(btn);
    }
    return panel;
  }
}
