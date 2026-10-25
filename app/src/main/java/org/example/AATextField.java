package org.example;

import javax.swing.JTextField;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** A JTextField that always renders its text antialiased, regardless of Look and Feel. */
public class AATextField extends JTextField {
  public AATextField() {
    super();
  }

  public AATextField(int columns) {
    super(columns);
  }

  public AATextField(String text, int columns) {
    super(text, columns);
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
