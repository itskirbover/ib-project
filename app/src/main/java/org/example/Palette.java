package org.example;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Shared dynamic theming: colors are always derived at paint time from the
 * active window background so cells darken it slightly; conflict and no-time
 * states come from the same palette. Also hosts the small UI helpers reused
 * across views (scroll panes, dialogs, antialiased painting).
 */
public final class Palette {
  public static final int DAYS = 5;
  public static final int PERIODS = 8;

  // Static timetable: 45-minute periods starting at 09:00 with fixed breaks.
  private static final int PERIOD_LENGTH_MINUTES = 45;
  private static final int[] BREAK_MINUTES_AFTER = { 5, 15, 5, 25, 5, 5, 5 };
  private static final String[] PERIOD_TIMES = computePeriodTimes();

  /** Static time label for a 0-based period, e.g. "9:00 AM - 9:45 AM". */
  public static String periodTime(int period) {
    return PERIOD_TIMES[period];
  }

  public static String[] dayNames() {
    return new String[] { "Mon", "Tue", "Wed", "Thu", "Fri" };
  }

  /** Period 1 is 09:00-09:45; breaks are 5/15/25 min per the school timetable. */
  private static String[] computePeriodTimes() {
    String[] times = new String[PERIODS];
    int minutes = 9 * 60; // first class starts at 09:00
    for (int i = 0; i < PERIODS; i++) {
      times[i] = formatClock(minutes) + " - " + formatClock(minutes + PERIOD_LENGTH_MINUTES);
      minutes += PERIOD_LENGTH_MINUTES;
      if (i < PERIODS - 1) {
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

  /**
   * Derives colors from the actual window background so cells blend with the
   * active Look and Feel: the fill is always slightly darker than the background.
   */
  public static Color windowBackground(Component c) {
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

  public static Color cellFill(Color bg) {
    return blend(bg == null ? Color.WHITE : bg, Color.BLACK, 0.10);
  }

  public static Color cellBorder(Color bg, Color fill) {
    return luminance(bg) < 0.5 ? blend(fill, Color.WHITE, 0.35) : blend(fill, Color.BLACK, 0.30);
  }

  /** Text palette derived from the fill brightness so labels stay readable. */
  public static Color[] textColors(Color bg) {
    boolean onDarkFill = luminance(cellFill(bg)) < 0.5;
    if (onDarkFill) {
      return new Color[] { new Color(235, 235, 235), new Color(190, 190, 190), new Color(150, 150, 150), new Color(165, 165, 165) };
    }
    return new Color[] { new Color(20, 20, 20), new Color(60, 60, 60), new Color(90, 90, 90), new Color(110, 110, 110) };
  }

  /** Muted (non-vibrant) red used for teacher no-time slots. */
  public static Color mutedRedFill(Color fill) {
    return blend(fill, new Color(122, 42, 42), 0.35);
  }

  /** Strong conflict red fill for clashing classes. */
  public static Color conflictFill(Color fill) {
    return blend(fill, new Color(224, 49, 49), 0.40);
  }

  /** Strong conflict red border. */
  public static Color conflictBorder(Color bg) {
    return blend(bg == null ? Color.WHITE : bg, new Color(178, 28, 28), 0.70);
  }

  /** Conflict red text that stays readable on the cell fill. */
  public static Color conflictText(Color bg) {
    boolean dark = luminance(cellFill(bg)) < 0.5;
    return dark ? new Color(255, 140, 140) : new Color(178, 28, 28);
  }

  public static double luminance(Color c) {
    return (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue()) / 255.0;
  }

  public static Color blend(Color a, Color b, double t) {
    t = Math.max(0, Math.min(1, t));
    return new Color(
        (int) Math.round(a.getRed() * (1 - t) + b.getRed() * t),
        (int) Math.round(a.getGreen() * (1 - t) + b.getGreen() * t),
        (int) Math.round(a.getBlue() * (1 - t) + b.getBlue() * t));
  }

  public static JLabel label(String text, int size, Color color, int style) {
    JLabel label = new JLabel(text == null ? "" : text);
    label.setFont(new Font("Arial", style, size));
    label.setForeground(color);
    label.setAlignmentX(Component.LEFT_ALIGNMENT);
    return label;
  }

  /** A non-opaque scroll pane with the shared border and scroll speed. */
  public static JScrollPane scrolled(java.awt.Component view) {
    JScrollPane scroll = new JScrollPane(view);
    scroll.setBorder(BorderFactory.createEmptyBorder());
    scroll.getVerticalScrollBar().setUnitIncrement(16);
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scroll.getViewport().setOpaque(false);
    scroll.setOpaque(false);
    return scroll;
  }

  public static void error(Component parent, String message, String title) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
  }

  public static void warn(Component parent, String message, String title) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
  }

  public static void info(Component parent, String message, String title) {
    JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
  }

  /** Left-hand period number drawn inside a circle. */
  public static JPanel periodBadge(int number) {
    JPanel badge = new JPanel() {
      @Override
      public java.awt.Dimension getPreferredSize() {
        return new java.awt.Dimension(50, 100);
      }

      @Override
      public java.awt.Dimension getMinimumSize() {
        return getPreferredSize();
      }

      @Override
      protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);
        if (!(g instanceof java.awt.Graphics2D g2dRaw)) {
          return;
        }
        java.awt.Graphics2D g2d = (java.awt.Graphics2D) g2dRaw.create();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING, java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int size = 34;
        int x = (getWidth() - size) / 2;
        int y = (getHeight() - size) / 2;

        Color win = windowBackground(this);
        Color fill = cellFill(win);

        g2d.setColor(fill);
        g2d.fillOval(x, y, size, size);
        g2d.setColor(cellBorder(win, fill));
        g2d.drawOval(x, y, size, size);

        g2d.setColor(Palette.luminance(fill) < 0.5 ? new Color(235, 235, 235) : new Color(40, 40, 40));
        g2d.setFont(new Font("Arial", Font.PLAIN, 13));
        FontMetrics fm = g2d.getFontMetrics();
        String label = String.valueOf(number);
        int textX = x + (size - fm.stringWidth(label)) / 2;
        int textY = y + (size - fm.getHeight()) / 2 + fm.getAscent();
        g2d.drawString(label, textX, textY);

        g2d.dispose();
      }
    };
    badge.setOpaque(false);
    return badge;
  }

  private Palette() {
  }
}
