package org.example;

import javax.swing.*;
import java.awt.*;

public class ScheduleWindow extends JFrame {
  private GridCellPanel[][] cells = new GridCellPanel[8][7];

  public ScheduleWindow() {
    this(null);
  }

  public ScheduleWindow(java.util.List<java.util.List<String>> importedData) {
    setTitle("Schedule Editor");
    setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

    JPanel contentPanel = new JPanel();
    contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    contentPanel.setLayout(new GridLayout(8, 7, 10, 10)); // 8 rows (y), 7 columns (x)

    for (int r = 0; r < 8; r++) {
      for (int c = 0; c < 7; c++) {
        String cellText = "";
        if (importedData != null && r < importedData.size()) {
          java.util.List<String> rowData = importedData.get(r);
          if (c < rowData.size()) {
            cellText = rowData.get(c);
          }
        }
        cells[r][c] = new GridCellPanel(cellText);
        contentPanel.add(cells[r][c]);
      }
    }

    add(contentPanel);

    // Set minimum size on the window to prevent it from resizing below the cells'
    // minimum size requirements.
    pack();
    setMinimumSize(getPreferredSize());
    setLocationRelativeTo(null); // Center on screen
  }

  private static class GridCellPanel extends JPanel {
    private JTextArea textArea;

    public GridCellPanel(String text) {
      setOpaque(false);
      setLayout(new BorderLayout());
      setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

      textArea = new JTextArea(text);
      textArea.setEditable(false);
      textArea.setFocusable(false);
      textArea.setLineWrap(true);
      textArea.setWrapStyleWord(true);
      textArea.setOpaque(false); // Make transparent so custom paint background shows
      textArea.setFont(new Font("Arial", Font.PLAIN, 11));

      add(textArea, BorderLayout.CENTER);
    }

    public void setText(String text) {
      textArea.setText(text);
    }

    @Override
    public Dimension getPreferredSize() {
      return new Dimension(160, 60);
    }

    @Override
    public Dimension getMinimumSize() {
      return new Dimension(160, 60);
    }

    @Override
    protected void paintComponent(Graphics g) {
      super.paintComponent(g);
      Graphics2D g2d = (Graphics2D) g.create();

      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      int w = getWidth();
      int h = getHeight();
      int arc = 15;

      g2d.setColor(new Color(245, 245, 245));
      g2d.fillRoundRect(0, 0, w - 1, h - 1, arc, arc);

      g2d.setColor(new Color(210, 210, 210));
      g2d.drawRoundRect(0, 0, w - 1, h - 1, arc, arc);

      g2d.dispose();
    }
  }
}
