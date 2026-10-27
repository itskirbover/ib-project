package org.example;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
import java.util.List;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.Box;
import javax.swing.JPanel;

/**
 * The shared 8x5 calendar grid: period badges per row, day headers, and cells
 * that stack every class running in that slot. Conflict classes get a strong
 * red border; a teacher's no-time slots render as muted red.
 */
public class CalendarGrid extends JPanel {
  private final List<Slot> slots;
  private final List<int[]> noTime;
  private final Map<Integer, List<String>> conflicts;
  /** Called with the day, period and the classes occupying that cell. */
  private final transient CellClick onCellClick;

  public interface CellClick {
    void on(int day, int period, List<Slot> slotsInCell, java.awt.Component source, boolean rightClick);
  }

  public CalendarGrid(List<Slot> slots, List<int[]> noTime, Map<Integer, List<String>> conflicts,
      CellClick onCellClick) {
    this.slots = slots;
    this.noTime = noTime;
    this.conflicts = conflicts;
    this.onCellClick = onCellClick;
    setOpaque(false);
    setLayout(new GridBagLayout());
    build();
  }

  private boolean isNoTime(int day, int period) {
    return noTime != null && noTime.stream().anyMatch(b -> b[0] == day && b[1] == period);
  }

  private List<Slot> cellSlots(int day, int period) {
    List<Slot> result = new ArrayList<>();
    for (Slot slot : slots) {
      if (slot.day() == day && slot.period() == period) {
        result.add(slot);
      }
    }
    return result;
  }

  private void build() {
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.gridy = 0;
    gbc.gridx = 0;
    add(new JPanel(), gbc);
    String[] dayNames = Palette.dayNames();
    for (int d = 0; d < Palette.DAYS; d++) {
      gbc.gridx = d + 1;
      JPanel head = new JPanel();
      head.setOpaque(false);
      head.add(Palette.label(dayNames[d], 12, Palette.textColors(Palette.windowBackground(this))[1],
          Font.BOLD));
      add(head, gbc);
    }

    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.BOTH;
    for (int p = 0; p < Palette.PERIODS; p++) {
      Insets insets = new Insets(4, 4, 4, 4);
      gbc.gridy = p + 1;
      gbc.weightx = 0.0;
      gbc.gridx = 0;
      gbc.insets = insets;
      add(Palette.periodBadge(p + 1), gbc);
      gbc.weightx = 1.0;
      for (int d = 0; d < Palette.DAYS; d++) {
        gbc.gridx = d + 1;
        add(new Cell(d, p), gbc);
      }
    }
  }

  private class Cell extends JPanel {
    private final int day;
    private final int period;

    Cell(int day, int period) {
      this.day = day;
      this.period = period;
      setOpaque(false);
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
      setBorder(javax.swing.BorderFactory.createEmptyBorder(6, 8, 6, 8));
      setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
      addMouseListener(new java.awt.event.MouseAdapter() {
        @Override
        public void mousePressed(java.awt.event.MouseEvent e) {
          onCellClick.on(day, period, cellSlots(day, period), Cell.this,
              javax.swing.SwingUtilities.isRightMouseButton(e));
        }
      });
      buildContent();
    }

    private boolean cellIsNoTime() {
      return isNoTime(day, period);
    }

    private void buildContent() {
      Color bg = Palette.windowBackground(this);
      Color[] palette = Palette.textColors(bg);
      List<Slot> inCell = cellSlots(day, period);
      if (cellIsNoTime() && inCell.isEmpty()) {
        add(Box.createVerticalGlue());
        add(centeredLabel("Unavailable", 10, palette[2], Font.PLAIN));
        add(Box.createVerticalGlue());
        return;
      }
      if (inCell.isEmpty()) {
        return;
      }

      // Multi-class slots: keep the cell compact — one subject + teacher line per
      // class, a Conflict chip when it clashes; details live in the tooltip.
      boolean multi = inCell.size() > 1;
      StringBuilder tooltip = new StringBuilder();
      if (!multi) {
        add(centeredLabel(Palette.periodTime(period), 9, palette[3], Font.PLAIN));
        add(Box.createVerticalStrut(3));
        Slot slot = inCell.get(0);
        boolean conflict = conflicts.containsKey(slot.id());
        add(centeredLabel(slot.subject(), 12, conflict ? Palette.conflictText(bg) : palette[0], Font.BOLD));
        if (!slot.teacher().isEmpty()) {
          add(centeredLabel(slot.teacher(), 10, palette[1], Font.PLAIN));
        }
        if (!slot.grade().isEmpty()) {
          add(centeredLabel(slot.grade(), 10, palette[2], Font.PLAIN));
        }
        if (!slot.room().isEmpty()) {
          add(centeredLabel(slot.room(), 10, palette[3], Font.PLAIN));
        }
        if (conflict) {
          add(Box.createVerticalStrut(2));
          add(centeredLabel(conflictLine(slot), 9, Palette.conflictText(bg), Font.PLAIN));
        }
        tooltip.append(tooltipLine(slot));
      } else {
        boolean anyConflict = false;
        for (Slot slot : inCell) {
          boolean conflict = conflicts.containsKey(slot.id());
          anyConflict |= conflict;
          add(centeredLabel(slot.subject(), 11, conflict ? Palette.conflictText(bg) : palette[0], Font.BOLD));
          if (!slot.teacher().isEmpty()) {
            add(centeredLabel(slot.teacher(), 9, palette[1], Font.PLAIN));
          }
          if (conflict) {
            add(centeredLabel("Conflict", 9, Palette.conflictText(bg), Font.BOLD));
          }
          add(Box.createVerticalStrut(2));
          if (tooltip.length() > 0) {
            tooltip.append("\n\n");
          }
          tooltip.append(tooltipLine(slot));
        }
        if (anyConflict) {
          add(Box.createVerticalStrut(1));
          tooltip.append("\n");
        }
      }
      setToolTipText(tooltip.toString());
    }

    private String conflictLine(Slot slot) {
      List<String> reasons = conflicts.get(slot.id());
      if (reasons == null || reasons.isEmpty()) {
        return "Conflict";
      }
      String reason = reasons.get(0);
      return reason.length() > 34 ? "Conflict" : reason;
    }

    private String tooltipLine(Slot slot) {
      StringBuilder line = new StringBuilder(slot.subject());
      if (!slot.teacher().isEmpty()) {
        line.append(" · ").append(slot.teacher());
      }
      if (!slot.grade().isEmpty()) {
        line.append(" · ").append(slot.grade());
      }
      if (!slot.room().isEmpty()) {
        line.append(" · ").append(slot.room());
      }
      line.append("\n").append(Palette.dayNames()[slot.day()]).append(", ").append(Palette.periodTime(slot.period()));
      List<String> reasons = conflicts.get(slot.id());
      if (reasons != null) {
        for (String reason : reasons) {
          line.append("\nConflict: ").append(reason);
        }
      }
      return line.toString();
    }

    private javax.swing.JLabel centeredLabel(String text, int size, Color color, int style) {
      javax.swing.JLabel label = Palette.label(text, size, color, style);
      label.setAlignmentX(Component.CENTER_ALIGNMENT);
      return label;
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(168, 96);
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

      int w = getWidth() - 1;
      int h = getHeight() - 1;
      int arc = 15;
      Color bg = Palette.windowBackground(this);
      Color fill = Palette.cellFill(bg);
      boolean noTime = cellIsNoTime();
      boolean conflict = cellSlots(day, period).stream().anyMatch(s -> conflicts.containsKey(s.id()));

      if (conflict) {
        fill = Palette.conflictFill(fill);
      } else if (!cellSlots(day, period).isEmpty() && noTime) {
        // Class scheduled on a no-time slot -> muted red class row.
        fill = Palette.mutedRedFill(fill);
      } else if (cellSlots(day, period).isEmpty() && noTime) {
        fill = Palette.mutedRedFill(fill);
      }

      g2d.setColor(fill);
      g2d.fillRoundRect(0, 0, w, h, arc, arc);
      g2d.setColor(conflict ? Palette.conflictBorder(bg) : Palette.cellBorder(bg, fill));
      g2d.drawRoundRect(0, 0, w, h, arc, arc);
      g2d.dispose();
    }
  }
}
