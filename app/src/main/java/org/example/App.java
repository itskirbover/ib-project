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
      // TODO change this so it just uses the regular windows LNF
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
    importBtn.addActionListener(e -> System.out.println("Import Schedule clicked"));
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
