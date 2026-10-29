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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

/**
 * Home view: every subject in one list, each row with separate Teachers and
 * Students accordions. Clicking a subject row folds it open; the Timetable
 * button on the right opens that subject's own calendar.
 */
public class SubjectView extends JPanel {
  private static final Set<String> expanded = new HashSet<>();

  public static JComponent create() {
    SubjectView view = new SubjectView();
    view.setLayout(new BorderLayout());
    view.setOpaque(false);

    JPanel list = new JPanel();
    list.setOpaque(false);
    list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
    list.setBorder(BorderFactory.createEmptyBorder(6, 10, 12, 10));

    try {
      for (Subject subject : SubjectDao.all()) {
        list.add(row(subject));
        list.add(Box.createVerticalStrut(6));
      }
    } catch (Exception ex) {
      list.add(Palette.label("Database error: " + ex.getMessage(), 12, Color.RED, Font.PLAIN));
    }

    JScrollPane scroll = new JScrollPane(list);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getViewport().setOpaque(false);
    scroll.setOpaque(false);
    view.add(scroll, BorderLayout.CENTER);
    return view;
  }

  private static JPanel row(Subject subject) {
    String name = subject.getName();
    List<Teacher> teachers = Views.teachers.stream()
        .filter(t -> t.getCourses().stream().anyMatch(s -> s.getName().equals(name)))
        .sorted(Comparator.comparing(Teacher::getName))
        .toList();
    List<Student> students = Views.studentsOfSubject(name).stream()
        .sorted(Comparator.comparing(Student::getName))
        .toList();
    List<Slot> slots = Views.slotsOfSubject(name).stream()
        .sorted(Comparator.comparingInt(Slot::day).thenComparingInt(Slot::period))
        .toList();

    JPanel wrap = new JPanel(new GridBagLayout());
    wrap.setOpaque(false);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.insets = new Insets(0, 0, 0, 0);
    wrap.add(header(name), gbc);

    if (expanded.contains(name)) {
      gbc.gridy++;
      gbc.insets = new Insets(6, 18, 0, 8);
      wrap.add(accordion(name, teachers, students, slots), gbc);
    }
    return wrap;
  }

  /** Rounded subject row: left side = fold/unfold on click, right = Timetable button. */
  private static JPanel header(String name) {
    JPanel panel = new JPanel(new BorderLayout(10, 0)) {
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

    JPanel nameSide = new JPanel(new BorderLayout());
    nameSide.setOpaque(false);
    nameSide.add(Palette.label(name, 14, texts[0], Font.BOLD), BorderLayout.CENTER);
    nameSide.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        toggle(name);
      }
    });
    panel.add(nameSide, BorderLayout.CENTER);

    JButton timetable = new javax.swing.JButton("Timetable");
    timetable.addActionListener(e -> Views.shell.showCard("Subject — " + name,
        () -> SubjectCalendarView.create(name)));
    panel.add(timetable, BorderLayout.EAST);
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

  /** Expanded part: Teachers and Students sections of visual rows. */
  private static JComponent accordion(String name, List<Teacher> teachers, List<Student> students,
      List<Slot> slots) {
    JPanel panel = new JPanel();
    panel.setOpaque(false);
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    panel.setBorder(BorderFactory.createEmptyBorder(0, 12, 4, 4));
    Color bg = Palette.windowBackground(panel);
    Color[] texts = Palette.textColors(bg);

    panel.add(Palette.label("Teachers", 12, texts[2], Font.BOLD));
    if (teachers.isEmpty()) {
      panel.add(Palette.label("No teachers teach this subject yet.", 11, texts[3], Font.PLAIN));
    }
    for (Teacher teacher : teachers) {
      panel.add(memberRow(teacher.getName(),
          teacher.getCourses().size() + " subject(s) · "
              + teacher.countBlockedSlots() + " unavailable slots", true));
      panel.add(Box.createVerticalStrut(4));
    }
    panel.add(Box.createVerticalStrut(8));
    panel.add(Palette.label("Students", 12, texts[2], Font.BOLD));
    if (students.isEmpty()) {
      panel.add(Palette.label("No students take this subject yet.", 11, texts[3], Font.PLAIN));
    }
    for (Student student : students) {
      panel.add(memberRow(student.getName(), student.getGrade(), false));
      panel.add(Box.createVerticalStrut(4));
    }
    panel.add(Box.createVerticalStrut(8));
    panel.add(Palette.label("Periods", 12, texts[2], Font.BOLD));
    if (slots.isEmpty()) {
      panel.add(Palette.label("Not scheduled yet.", 11, texts[3], Font.PLAIN));
    }
    for (Slot slot : slots) {
      panel.add(slotRow(slot));
      panel.add(Box.createVerticalStrut(4));
    }
    return panel;
  }

  /** Visual member row: left-click opens their calendar, right-click edits. */
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
        g2d.setColor(Palette.blend(fill, bg, 0.5));
        g2d.fillRoundRect(0, 0, w, h, 10, 10);
        g2d.setColor(Palette.cellBorder(bg, fill));
        g2d.drawRoundRect(0, 0, w, h, 10, 10);
        g2d.dispose();
      }
    };
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    Color[] texts = Palette.textColors(Palette.windowBackground(row));
    JPanel textSide = new JPanel();
    textSide.setOpaque(false);
    textSide.setLayout(new BoxLayout(textSide, BoxLayout.Y_AXIS));
    textSide.add(Palette.label(memberName, 12, texts[0], Font.PLAIN));
    textSide.add(Palette.label(caption, 10, texts[3], Font.PLAIN));
    row.add(textSide, BorderLayout.CENTER);
    row.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (javax.swing.SwingUtilities.isRightMouseButton(e)) {
          JPopupMenu menu = new JPopupMenu();
          JMenuItem edit = new JMenuItem("Edit...");
          edit.addActionListener(ev -> MembersView.edit(memberName));
          menu.add(edit);
          JMenuItem remove = new JMenuItem(teacher ? "Remove teacher..." : "Remove student...");
          remove.addActionListener(ev -> MembersView.remove(memberName));
          menu.add(remove);
          menu.show(row, e.getX(), e.getY());
          return;
        }
        Views.shell.showCard("Member — " + memberName, () -> MemberCalendarView.create(memberName));
      }
    });
    return row;
  }

  /** One scheduled period of this subject: click opens the class details. */
  private static JComponent slotRow(Slot slot) {
    String label = Palette.dayNames()[slot.day()] + " · " + slot.periodLabel()
        + (slot.grade().isEmpty() ? "" : " · " + slot.grade())
        + (slot.room().isEmpty() ? "" : " · " + slot.room());
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
        g2d.setColor(Palette.blend(fill, bg, 0.5));
        g2d.fillRoundRect(0, 0, w, h, 10, 10);
        g2d.setColor(Palette.cellBorder(bg, fill));
        g2d.drawRoundRect(0, 0, w, h, 10, 10);
        g2d.dispose();
      }
    };
    row.setOpaque(false);
    row.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
    row.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    Color[] texts = Palette.textColors(Palette.windowBackground(row));
    row.add(Palette.label(label, 12, texts[0], Font.PLAIN), BorderLayout.CENTER);
    row.addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        ClassView.open(slot);
      }
    });
    return row;
  }
}
