package org.example;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 * A JComboBox drawn with rounded corners matching the schedule cells: the
 * control fills a rounded rectangle and the popup rows render as rounded
 * pills via the shared antialiased renderer.
 */
public class AARoundedComboBox extends JComboBox<String> {
  private static final int ARC = 14;

  public AARoundedComboBox(String[] items) {
    super(items);
    setOpaque(false);
    setEditable(false);
    setUI(new RoundedUI());
  }

  @Override
  public void updateUI() {
    super.updateUI();
    setUI(new RoundedUI());
  }

  private class RoundedUI extends BasicComboBoxUI {
    @Override
    public void installUI(JComponent c) {
      super.installUI(c);
      comboBox.setOpaque(false);
      comboBox.setBorder(BorderFactory.createEmptyBorder(1, 10, 1, 4));
      comboBox.setBackground(ScheduleWindow.cellFill(comboBox.getBackground()));
      if (listBox != null) {
        listBox.setBackground(comboBox.getBackground());
        listBox.setForeground(comboBox.getForeground());
      }
    }

    @Override
    protected JButton createArrowButton() {
      JButton arrow = new JButton() {
        @Override
        public Dimension getPreferredSize() {
          return new Dimension(26, 30);
        }

        @Override
        public void paintComponent(Graphics g) {
          if (!(g instanceof Graphics2D g2dRaw)) {
            return;
          }
          JComboBox<?> combo = (JComboBox<?>) SwingUtilities.getAncestorOfClass(JComboBox.class, this);
          if (combo == null) {
            return;
          }
          Graphics2D g2d = (Graphics2D) g2dRaw.create();
          g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

          int w = getWidth();
          int h = getHeight();
          int size = Math.max(6, Math.min(w, h) / 3);
          int cx = w / 2;
          int cy = h / 2;
          int[] xs = { cx - size / 2, cx + size / 2, cx };
          int[] ys = { cy - size / 4, cy - size / 4, cy + size / 3 };
          g2d.setColor(combo.getForeground());
          g2d.fillPolygon(xs, ys, 3);
          g2d.dispose();
        }
      };
      arrow.setOpaque(false);
      arrow.setContentAreaFilled(false);
      arrow.setBorderPainted(false);
      arrow.setFocusPainted(false);
      arrow.setBorder(null);
      return arrow;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
      if (comboBox.isEditable() || !(g instanceof Graphics2D g2dRaw)) {
        super.paint(g, c);
        return;
      }
      Graphics2D g2d = (Graphics2D) g2dRaw.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      int w = comboBox.getWidth() - 1;
      int h = comboBox.getHeight() - 1;
      Color fill = comboBox.getBackground();
      Color border = ScheduleWindow.cellBorder(fill, fill);

      g2d.setColor(fill);
      g2d.fillRoundRect(0, 0, w, h, ARC, ARC);
      g2d.setColor(border);
      g2d.drawRoundRect(0, 0, w, h, ARC, ARC);

      paintCurrentValue(g2d, rectangleForCurrentValue(), false);
      g2d.dispose();
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
      // The rounded background is painted in paint(); nothing to do here.
    }
  }
}
