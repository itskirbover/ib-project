package org.example;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScheduleWindow extends JFrame {
  private static final int ROWS = 8;
  private static final int COLS = 5;
  private static final int SLOT_COUNT = ROWS * COLS;

  // Static timetable: 45-minute periods starting at 09:00 with fixed breaks.
  private static final int PERIOD_LENGTH_MINUTES = 45;
  private static final int[] BREAK_MINUTES_AFTER = { 5, 15, 5, 25, 5, 5, 5 };
  private static final String[] PERIOD_TIMES = computePeriodTimes();

  private final File file;
  private List<List<String>> data;
  private JPanel contentPanel;

  public ScheduleWindow(File file) throws IOException {
    this.file = file;
    this.data = normalize(ExcelReader.readExcel(file));
    seedRegistry();

    setTitle("Schedule Editor");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    contentPanel = new JPanel(new GridBagLayout());
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    rebuildGrid();

    add(contentPanel);

    // Set minimum size on the window to prevent it from resizing below the cells'
    // minimum size requirements.
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(null); // Center on screen
  }

  // ---- static timetable --------------------------------------------------

  /** Period 1 is 09:00-09:45; breaks are 5/15/25 min per the school timetable. */
  private static String[] computePeriodTimes() {
    String[] times = new String[ROWS];
    int minutes = 9 * 60; // first class starts at 09:00
    for (int i = 0; i < ROWS; i++) {
      times[i] = formatClock(minutes) + " - " + formatClock(minutes + PERIOD_LENGTH_MINUTES);
      minutes += PERIOD_LENGTH_MINUTES;
      if (i < ROWS - 1) {
        minutes += BREAK_MINUTES_AFTER[i];
      }
    }
    return times;
  }

  private static String formatClock(int minutesOfDay) {
    int hour = minutesOfDay / 60;
    int minute = minutesOfDay % 60;
    boolean pm = hour >= 12;
    int twelveHour = hour % 12 == 0 ? 12 : hour % 12;
    return twelveHour + ":" + (minute < 10 ? "0" : "") + minute + (pm ? " PM" : " AM");
  }

  // ---- spreadsheet <-> grid ----------------------------------------------

  /**
   * Maps any imported file to the current layout: a header row followed by
   * exactly ROWS*COLS data rows of [Subject, Grade, Teacher, Room]. Legacy
   * files containing a static "Time" column have that column dropped.
   */
  private static List<List<String>> normalize(List<List<String>> raw) {
    int timeColumn = -1;
    boolean hasHeader = false;
    if (!raw.isEmpty()) {
      for (int i = 0; i < raw.get(0).size(); i++) {
        if (cell(raw.get(0), i).trim().equalsIgnoreCase("Time")) {
          timeColumn = i;
          break;
        }
      }
      for (String header : raw.get(0)) {
        for (String expect : ExcelReader.COLUMN_HEADERS) {
          if (header != null && header.trim().equalsIgnoreCase(expect)) {
            hasHeader = true;
          }
        }
      }
    }

    // Slice out the Time column from every row of legacy files.
    List<List<String>> source = new ArrayList<>();
    for (List<String> row : raw) {
      List<String> copy = new ArrayList<>(row);
      if (timeColumn >= 0 && timeColumn < copy.size()) {
        copy.remove(timeColumn);
      }
      source.add(copy);
    }

    List<List<String>> normalized = new ArrayList<>();
    normalized.add(new ArrayList<>(List.of(ExcelReader.COLUMN_HEADERS)));
    for (int i = hasHeader ? 1 : 0; i < source.size(); i++) {
      List<String> d = source.get(i);
      normalized.add(new ArrayList<>(List.of(cell(d, 0), cell(d, 1), cell(d, 2), cell(d, 3))));
    }

    // The layout is positional: pad to a full grid and drop anything past it.
    while (normalized.size() < SLOT_COUNT + 1) {
      normalized.add(new ArrayList<>(List.of("", "", "", "")));
    }
    return new ArrayList<>(normalized.subList(0, SLOT_COUNT + 1));
  }

  private void seedRegistry() {
    for (int i = 1; i < data.size(); i++) {
      List<String> row = data.get(i);
      Registry.register(cell(row, 0), cell(row, 2), cell(row, 1));
    }
  }

  private static int slotIndex(int row, int col) {
    return 1 + row * COLS + col;
  }

  private void rebuildGrid() {
    contentPanel.removeAll();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.BOTH;
    gbc.weighty = 1.0;
    gbc.insets = new Insets(5, 5, 5, 5);

    for (int r = 0; r < ROWS; r++) {
      gbc.gridy = r;

      gbc.gridx = 0;
      gbc.weightx = 0.0;
      contentPanel.add(new RowNumberPanel(r + 1), gbc);

      gbc.weightx = 1.0;
      for (int c = 0; c < COLS; c++) {
        List<String> row = data.get(slotIndex(r, c));
        SchoolClass schoolClass = toSchoolClass(PERIOD_TIMES[r], cell(row, 0), cell(row, 1), cell(row, 2), cell(row, 3));
        gbc.gridx = c + 1;
        contentPanel.add(new GridCellPanel(r, c, schoolClass), gbc);
      }
    }
    contentPanel.revalidate();
    contentPanel.repaint();
  }

  private static String cell(List<String> row, int index) {
    return index < row.size() ? row.get(index) : "";
  }

  private static Map<String, Subject> subjectCache = new HashMap<>();
  private static Map<String, Teacher> teacherCache = new HashMap<>();

  private static SchoolClass toSchoolClass(String time, String subjectName, String grade, String teacherName, String room) {
    Subject subject = subjectCache.computeIfAbsent(subjectName == null ? "" : subjectName.trim(), Subject::new);
    Teacher teacher = teacherCache.computeIfAbsent(teacherName == null ? "" : teacherName.trim(), Teacher::new);
    SchoolClass schoolClass = new SchoolClass(subject, teacher);
    schoolClass.setTime(time);
    schoolClass.setGrade(grade == null ? "" : grade.trim());
    schoolClass.setRoom(room == null ? "" : room.trim());
    return schoolClass;
  }

  // ---- cell rendering ------------------------------------------------------

  private class GridCellPanel extends JPanel {
    private final int row;
    private final int col;
    private final SchoolClass schoolClass;

    GridCellPanel(int row, int col, SchoolClass schoolClass) {
      this.row = row;
      this.col = col;
      this.schoolClass = schoolClass;

      setOpaque(false);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          editCell(GridCellPanel.this.row, GridCellPanel.this.col);
        }
      });
      buildContent();
    }

    private void buildContent() {
      if (schoolClass != null && !schoolClass.isEmpty()) {
        // The time is static per period row and is only shown in non-empty cells.
        Color[] palette = labelColors(windowBackground(this));
        Color softer = palette[3];
        Color strong = palette[0];
        Color mid = palette[1];
        Color subtle = palette[2];
        add(makeLabel(schoolClass.getTime(), 10, softer, Font.PLAIN));
        add(Box.createVerticalStrut(4));
        add(makeLabel(schoolClass.getSubject().getName(), 12, strong, Font.BOLD));
        add(Box.createVerticalStrut(4));
        add(makeLabel(schoolClass.getGrade(), 11, mid, Font.PLAIN));
        add(makeLabel(schoolClass.getTeacher().getName(), 11, mid, Font.PLAIN));
        add(makeLabel(schoolClass.getRoom(), 11, subtle, Font.PLAIN));
      }
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(170, 100);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(170, 100);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (!(g instanceof Graphics2D g2dRaw)) {
        return;
      }
      Graphics2D g2d = (Graphics2D) g2dRaw.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int arc = 15;

      Color bg = windowBackground(this);
      Color fill = cellFill(bg);

      g2d.setColor(fill);
      g2d.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

      g2d.setColor(cellBorder(bg, fill));
      g2d.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

      g2d.dispose();
    }
  }

  /**
   * Derives colors from the actual window background so cells blend with the
   * active Look and Feel: the fill is always slightly darker than the background.
   */
  private static Color windowBackground(Component c) {
    JFrame win = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, c);
    if (win != null && win.getContentPane() != null && win.getContentPane().getBackground() != null) {
      return win.getContentPane().getBackground();
    }
    Container parent = c.getParent();
    if (parent != null && parent.getBackground() != null) {
      return parent.getBackground();
    }
    Color uiDefault = UIManager.getColor("Panel.background");
    return uiDefault != null ? uiDefault : Color.WHITE;
  }

  static Color cellFill(Color bg) {
    return blend(bg == null ? Color.WHITE : bg, Color.BLACK, 0.10);
  }

  static Color cellBorder(Color bg, Color fill) {
    return luminance(bg) < 0.5 ? blend(fill, Color.WHITE, 0.35) : blend(fill, Color.BLACK, 0.30);
  }

  /** Text palette derived from the fill brightness so labels stay readable. */
  private static Color[] labelColors(Color bg) {
    boolean onDarkFill = luminance(cellFill(bg)) < 0.5;
    if (onDarkFill) {
      return new Color[] { new Color(235, 235, 235), new Color(190, 190, 190), new Color(150, 150, 150), new Color(165, 165, 165) };
    }
    return new Color[] { new Color(20, 20, 20), new Color(60, 60, 60), new Color(90, 90, 90), new Color(110, 110, 110) };
  }

  private static AALabel makeLabel(String text, int size, Color color, int style) {
    AALabel label = new AALabel(text == null ? "" : text);
    label.setFont(new Font("Arial", style, size));
    label.setForeground(color);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  static double luminance(Color c) {
    return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
  }

  static Color blend(Color a, Color b, double t) {
    t = Math.max(0, Math.min(1, t));
    return new Color(
        (int) Math.round(a.getRed() * (1 - t) + b.getRed() * t),
        (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
        (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
  }

  /** Left-hand period number drawn inside a circle. */
  private static class RowNumberPanel extends JPanel {
    private final int number;

    RowNumberPanel(int number) {
      this.number = number;
      setOpaque(false);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(50, 100);
    }

    @Override
    public Dimension getMinimumSize() {
      return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      if (!(g instanceof Graphics2D g2dRaw)) {
        return;
      }
      Graphics2D g2d = (Graphics2D) g2dRaw.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      int size = 34;
      int x = (getWidth() - size) / 2;
      int y = (getHeight() - size) / 2;

      Color bg = windowBackground(this);
      Color fill = cellFill(bg);

      g2d.setColor(fill);
      g2d.fillOval(x, y, size, size);
      g2d.setColor(cellBorder(bg, fill));
      g2d.drawOval(x, y, size, size);

      g2d.setColor(luminance(fill) < 0.5 ? new Color(235, 235, 235) : new Color(40, 40, 40));
      g2d.setFont(new Font("Arial", Font.PLAIN, 13));
      FontMetrics fm = g2d.getFontMetrics();
      String label = String.valueOf(number);
      int textX = x + (size - fm.stringWidth(label)) / 2;
      int textY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
      g2d.drawString(label, textX, textY);

      g2d.dispose();
    }
  }

  // ---- editing -------------------------------------------------------------

  private enum CatalogKind {
    SUBJECT, TEACHER
  }

  /** Modal single-field input dialog rendered with antialiased text. */
  private static String prompt(Component parent, String message) {
    AATextField field = new AATextField(16);
    JPanel panel = new JPanel(new BorderLayout(8, 0));
    panel.add(new AALabel(message + ":"), BorderLayout.WEST);
    panel.add(field, BorderLayout.CENTER);
    int result = JOptionPane.showConfirmDialog(parent, panel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    return result == JOptionPane.OK_OPTION ? field.getText() : null;
  }

  /** Opens a modal editor for the class in the given grid cell and persists changes. */
  private void editCell(int row, int col) {
    int index = slotIndex(row, col);
    List<String> current = data.get(index);
    String subject = cell(current, 0);
    String grade = cell(current, 1);
    String teacher = cell(current, 2);
    String room = cell(current, 3);
    Registry.register(subject, teacher, grade);

    JComboBox<String> subjectBox = comboBox(Registry.subjectItems(), subject);
    JComboBox<String> gradeBox = comboBox(Registry.gradeItems(), grade);
    JComboBox<String> teacherBox = comboBox(Registry.teacherItems(), teacher);
    AATextField roomField = new AATextField(room, 10);

    attachAddListener(subjectBox, CatalogKind.SUBJECT);
    attachAddListener(teacherBox, CatalogKind.TEACHER);

    JPanel panel = new JPanel(new GridBagLayout());
    GridBagConstraints d = new GridBagConstraints();
    d.insets = new Insets(4, 4, 4, 4);
    d.anchor = GridBagConstraints.WEST;
    JComponent[] inputs = { subjectBox, gradeBox, teacherBox, roomField };
    String[] labels = { "Subject", "Grade", "Teacher", "Room" };
    for (int i = 0; i < labels.length; i++) {
      d.gridx = 0;
      d.gridy = i;
      panel.add(new AALabel(labels[i] + ":"), d);
      d.gridx = 1;
      panel.add(inputs[i], d);
    }

    // The window title shows the static row time; time itself is not editable.
    int result = JOptionPane.showConfirmDialog(this, panel, "Edit Class ("
        + PERIOD_TIMES[row] + ")", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    if (result != JOptionPane.OK_OPTION) {
      return;
    }

    String newSubject = boxValue(subjectBox);
    String newGrade = boxValue(gradeBox);
    String newTeacher = boxValue(teacherBox);
    String newRoom = roomField.getText().trim();

    data.set(index, new ArrayList<>(List.of(newSubject, newGrade, newTeacher, newRoom)));
    try {
      ExcelReader.writeSchedule(file, data);
      rebuildGrid();
    } catch (IOException ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this, "Failed to save schedule: " + ex.getMessage(),
          "Save Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private static String boxValue(JComboBox<String> box) {
    Object selected = box.getSelectedItem();
    return selected == null ? "" : selected.toString().trim();
  }

  private JComboBox<String> comboBox(List<String> items, String initial) {
    JComboBox<String> box = new AARoundedComboBox(items.toArray(new String[0]));
    box.setRenderer(new AAComboBoxRenderer());
    if (initial != null && !initial.isBlank()) {
      box.setSelectedItem(initial);
    } else {
      box.setSelectedIndex(-1);
    }
    return box;
  }

  private void attachAddListener(JComboBox<String> box, CatalogKind kind) {
    boolean[] updating = { false };
    Object[] restore = { box.getSelectedItem() };
    box.addActionListener(e -> {
      if (updating[0]) {
        return;
      }
      String sentinel = kind == CatalogKind.SUBJECT ? Registry.ADD_SUBJECT : Registry.ADD_TEACHER;
      if (!sentinel.equals(box.getSelectedItem())) {
        restore[0] = box.getSelectedItem();
        return;
      }
      updating[0] = true;
      try {
        String prompt = kind == CatalogKind.SUBJECT ? "Subject name" : "Teacher name";
        String name = prompt(this, prompt);
        if (name != null && !name.trim().isBlank()) {
          name = name.trim();
          if (kind == CatalogKind.SUBJECT) {
            Registry.addSubject(name);
          } else {
            Registry.addTeacher(name);
          }
          box.removeAllItems();
          List<String> items = kind == CatalogKind.SUBJECT ? Registry.subjectItems() : Registry.teacherItems();
          for (String item : items) {
            box.addItem(item);
          }
          box.setSelectedItem(name);
          restore[0] = name;
        } else {
          box.setSelectedItem(restore[0]);
        }
      } finally {
        updating[0] = false;
      }
    });
  }

  private static class AAComboBoxRenderer extends AALabel implements ListCellRenderer<String> {
    private static final int ARC = 10;

    AAComboBoxRenderer() {
      setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
        boolean selected, boolean focus) {
      setText(value == null ? "" : value);
      setBackground(selected ? list.getSelectionBackground() : list.getBackground());
      setForeground(selected ? list.getSelectionForeground() : list.getForeground());
      setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
      return this;
    }

    @Override
    public void paintComponent(Graphics g) {
      if (!(g instanceof Graphics2D g2dRaw)) {
        super.paintComponent(g);
        return;
      }
      Graphics2D g2d = (Graphics2D) g2dRaw.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      g2d.setColor(getBackground());
      g2d.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);

      try {
        super.paintComponent(g2d);
      } finally {
        g2d.dispose();
      }
    }
  }
}
