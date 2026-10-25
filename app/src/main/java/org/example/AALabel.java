package org.example;

import javax.swing.JLabel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * A JLabel that always renders its text antialiased by forcing the hints onto
 * the Graphics used for painting. This works regardless of Look and Feel
 * (Qt/GTK themes ignore the awt.useSystemAAFontSettings system property).
 */
public class AALabel extends JLabel {
  public AALabel() {
    super();
  }

  public AALabel(String text) {
    super(text);
  }

  @Override
  public void paintComponent(Graphics g) {
    if (!(g instanceof Graphics2D g2dRaw)) {
      super.paintComponent(g);
      return;
    }
    Graphics2D g2d = (Graphics2D) g2dRaw.create();
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    try {
      super.paintComponent(g2d);
    } finally {
      g2d.dispose();
    }
  }
}
