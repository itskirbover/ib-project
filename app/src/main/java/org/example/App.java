package org.example;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

public class App {
  static JFrame frame = new JFrame("Patron");
  static JPanel appPanel;

  public String getGreeting() {
    return "Hello World!";
  }

  public static void main(String[] args) {
    // Antialiased text must be enabled before AWT initializes.
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
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

    AAButton importBtn = new AAButton("Import Schedule");
    AAButton createBtn = new AAButton("Create Schedule");
    AAButton optionsBtn = new AAButton("Options");

    importBtn.addActionListener(e -> {
      System.out.println("Import Schedule clicked");
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Select Schedule Excel File");

      // Filter for Excel files
      fileChooser.setFileFilter(new FileNameExtensionFilter(
          "Excel Spreadsheets (*.xls, *.xlsx)", "xls", "xlsx"));

      if (fileChooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION) {
        openSchedule(fileChooser.getSelectedFile());
      }
    });

    createBtn.addActionListener(e -> {
      System.out.println("Create Schedule clicked");
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setDialogTitle("Create New Schedule");
      fileChooser.setSelectedFile(new File("schedule.xlsx"));
      fileChooser.setFileFilter(new FileNameExtensionFilter("Excel Spreadsheets (*.xlsx)", "xlsx"));

      if (fileChooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".xlsx")) {
          file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }
        try {
          ExcelReader.createBlankTemplate(file);
          openSchedule(file);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(frame,
              "Error creating schedule file: " + ex.getMessage(),
              "Create Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
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

  private static void openSchedule(File file) {
    try {
      ScheduleWindow scheduleWindow = new ScheduleWindow(file);
      // The start menu hides while editing and comes back when the window closes.
      scheduleWindow.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(WindowEvent e) {
          frame.setVisible(true);
          frame.toFront();
        }
      });
      frame.setVisible(false);
      scheduleWindow.setVisible(true);
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(frame,
          "Error reading Excel file: " + ex.getMessage(),
          "Import Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }
}
