package org.example;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import java.awt.BorderLayout;
import java.awt.Font;

public class App {

  public static void main(String[] args) {
    // Antialiased text must be enabled before AWT initializes.
    System.setProperty("awt.useSystemAAFontSettings", "on");
    System.setProperty("swing.aatext", "true");
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

    // Embedded database: boot with a small progress note, then open the app.
    JDialog splash = splashDialog("Starting database...");
    SwingWorker<Integer, Void> boot = new SwingWorker<>() {
      @Override
      protected Integer doInBackground() {
        try {
          Database.start();
          Runtime.getRuntime().addShutdownHook(new Thread(Database::stop));
          return 0;
        } catch (Exception e) {
          e.printStackTrace();
          return 1;
        }
      }

      @Override
      protected void done() {
        splash.dispose();
        boolean ok;
        try {
          ok = get() == 0;
        } catch (Exception e) {
          ok = false;
        }
        if (ok) {
          Registry.load();
          Views.init();
        } else {
          JOptionPane.showMessageDialog(null,
              "Could not start the embedded database. Check ~/.patron/ for a stale\n"
                  + "server process and try again.",
              "Database Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    };
    splash.setVisible(true);
    boot.execute();
  }

  private static JDialog splashDialog(String message) {
    JDialog dialog = new JDialog();
    dialog.setUndecorated(true);
    JPanel panel = new JPanel(new BorderLayout(16, 16));
    panel.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
    JLabel label = new JLabel(message);
    label.setFont(new Font("Arial", Font.PLAIN, 14));
    panel.add(label, BorderLayout.CENTER);
    dialog.setContentPane(panel);
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    return dialog;
  }
}
