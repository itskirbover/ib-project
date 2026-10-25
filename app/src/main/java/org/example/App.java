package org.example;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import java.awt.GridLayout;

public class App {
  static JFrame frame = new JFrame("Patron");
  static JPanel appPanel;

  public String getGreeting() {
    return "Hello World!";
  }

  public static void main(String[] args) {
    System.out.println(new App().getGreeting());
    try {
      // TODO change this so it just uses the regular windows LNF for production
      UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
    } catch (Exception e) {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      } catch (Exception e1) {
        e1.printStackTrace();
      }
    }

    appPanel = new JPanel();
    appPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    appPanel.setLayout(new GridLayout(3, 1, 10, 10));

    JButton importBtn = new JButton("Import Schedule");
    JButton createBtn = new JButton("Create Schedule");
    JButton optionsBtn = new JButton("Options");

    // stubs
    importBtn.addActionListener(e -> {
      System.out.println("Import Schedule clicked");
      javax.swing.JFileChooser fileChooser = new javax.swing.JFileChooser();
      fileChooser.setDialogTitle("Select Schedule Excel File");

      // Filter for Excel files
      javax.swing.filechooser.FileNameExtensionFilter filter = new javax.swing.filechooser.FileNameExtensionFilter(
          "Excel Spreadsheets (*.xls, *.xlsx)", "xls", "xlsx");
      fileChooser.setFileFilter(filter);

      int userSelection = fileChooser.showOpenDialog(frame);
      if (userSelection == javax.swing.JFileChooser.APPROVE_OPTION) {
        java.io.File file = fileChooser.getSelectedFile();
        try {
          java.util.List<java.util.List<String>> importedData = ExcelReader.readExcel(file);
          ScheduleWindow scheduleWindow = new ScheduleWindow(importedData);
          scheduleWindow.setVisible(true);
        } catch (Exception ex) {
          ex.printStackTrace();
          javax.swing.JOptionPane.showMessageDialog(frame,
              "Error reading Excel file: " + ex.getMessage(),
              "Import Error",
              javax.swing.JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    createBtn.addActionListener(e -> {
      System.out.println("Create Schedule clicked");
      ScheduleWindow scheduleWindow = new ScheduleWindow();
      scheduleWindow.setVisible(true);
    });
    optionsBtn.addActionListener(e -> System.out.println("Options clicked"));

    appPanel.add(importBtn);
    appPanel.add(createBtn);
    appPanel.add(optionsBtn);

    frame.add(appPanel);
    frame.setSize(350, 250);
    frame.setResizable(false);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationRelativeTo(null); // Centers window on screen
    frame.setVisible(true);
  }
}
