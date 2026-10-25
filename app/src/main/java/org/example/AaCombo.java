package org.example;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

/** Shared dropdown + prompt helpers. */
public final class AaCombo {
  public enum CatalogKind {
    SUBJECT, TEACHER
  }

  /** Modal single-field input dialog. */
  public static String prompt(Component parent, String message) {
    JTextField field = new JTextField(16);
    JPanel panel = new JPanel(new BorderLayout(8, 0));
    panel.add(new JLabel(message + ":"), BorderLayout.WEST);
    panel.add(field, BorderLayout.CENTER);
    int result = JOptionPane.showConfirmDialog(parent, panel, message,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
    return result == JOptionPane.OK_OPTION ? field.getText() : null;
  }

  public static JComboBox<String> comboBox(List<String> items, String initial) {
    JComboBox<String> box = new JComboBox<>(items.toArray(new String[0]));
    if (initial != null && !initial.isBlank()) {
      box.setSelectedItem(initial);
    } else {
      box.setSelectedIndex(-1);
    }
    return box;
  }

  /** Makes the "+ Add subject/teacher..." sentinel work, persisting via Registry. */
  public static void attachAddListener(JComboBox<String> box, CatalogKind kind) {
    boolean[] updating = { false };
    Object[] restore = { box.getSelectedItem() };
    box.addActionListener(e -> {
      if (updating[0]) {
        return;
      }
      String sentinel = kind == CatalogKind.SUBJECT ? Registry.ADD_SUBJECT : Registry.ADD_TEACHER;
      if (!sentinel.equals(box.getSelectedItem())) {
        restore[0] = box.getSelectedItem();
        return;
      }
      updating[0] = true;
      try {
        String prompt = kind == CatalogKind.SUBJECT ? "Subject name" : "Teacher name";
        String name = prompt(box, prompt);
        if (name != null && !name.trim().isBlank()) {
          name = name.trim();
          if (kind == CatalogKind.SUBJECT) {
            Registry.addSubject(name);
          } else {
            Registry.addTeacher(name);
          }
          box.removeAllItems();
          List<String> items = kind == CatalogKind.SUBJECT ? Registry.subjectItems() : Registry.teacherItems();
          for (String item : items) {
            box.addItem(item);
          }
          box.setSelectedItem(name);
          restore[0] = name;
        } else {
          box.setSelectedItem(restore[0]);
        }
      } finally {
        updating[0] = false;
      }
    });
  }

  public static String boxValue(JComboBox<String> box) {
    Object selected = box.getSelectedItem();
    return selected == null ? "" : selected.toString().trim();
  }

  public static JPanel labeled(JComponent input, String label) {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setOpaque(false);
    panel.add(new JLabel(label + ":"), new GridBagConstraints() {
      {
        gridx = 0;
        anchor = GridBagConstraints.WEST;
        insets = new Insets(4, 4, 4, 4);
      }
    });
    panel.add(input, new GridBagConstraints() {
      {
        gridx = 1;
        anchor = GridBagConstraints.WEST;
        insets = new Insets(4, 4, 4, 4);
      }
    });
    return panel;
  }

  private AaCombo() {
  }
}
