package org.example;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 * Single-window shell: a top bar (back button, section switches) and a card
 * area whose content is swapped between views. Every card is supplied by a
 * factory so re-renders after database writes rebuild the same view.
 */
public class MainShell extends JFrame {
  private record Card(String title, Supplier<JComponent> factory) {
  }

  private final Deque<Card> history = new ArrayDeque<>();
  private final JPanel cardArea = new JPanel(new BorderLayout());
  private final JPanel topBar = new JPanel(new BorderLayout(10, 0));
  private Card current;

  MainShell() {
    super("Patron");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    topBar.setOpaque(false);
    topBar.setBorder(BorderFactory.createEmptyBorder(10, 14, 4, 14));
    cardArea.setBorder(BorderFactory.createEmptyBorder(0, 12, 12, 12));
    add(topBar, BorderLayout.NORTH);
    add(cardArea, BorderLayout.CENTER);
    setSize(1060, 720);
    setMinimumSize(new Dimension(1020, 660));
    setLocationRelativeTo(null);
    setVisible(true);
  }

  /* ---- navigation ---- */

  void showCard(String title, Supplier<JComponent> factory) {
    if (current != null) {
      history.push(current);
    }
    open(new Card(title, factory));
  }

  void showHome() {
    current = null;
    history.clear();
    open(homeCard());
  }

  void back() {
    open(history.isEmpty() ? homeCard() : history.pop());
  }

  void showTimetable() {
    showCard("Timetable", TimetableView::create);
  }

  void rerenderCurrent() {
    if (current != null) {
      SwingUtilities.invokeLater(() -> open(current));
    }
  }

  /* ---- plumbing ---- */

  private static Card homeCard() {
    return new Card("Subjects", SubjectView::create);
  }

  private void open(Card card) {
    current = card;
    SwingUtilities.invokeLater(() -> {
      setTitle("Patron — " + card.title());
      buildTopBar();
      cardArea.removeAll();
      cardArea.add(card.factory().get(), BorderLayout.CENTER);
      cardArea.revalidate();
      cardArea.repaint();
    });
  }

  private void buildTopBar() {
    topBar.removeAll();
    JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    leftPanel.setOpaque(false);
    if (current != null && !history.isEmpty()) {
      JButton back = new JButton("Back");
      back.addActionListener(e -> back());
      leftPanel.add(back);
    }
    topBar.add(leftPanel, BorderLayout.WEST);

    JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
    navPanel.setOpaque(false);
    JButton subjects = new JButton("Subjects");
    JButton timetable = new JButton("Timetable");
    JButton members = new JButton("Members");
    subjects.addActionListener(e -> showCard("Subjects", SubjectView::create));
    timetable.addActionListener(e -> showTimetable());
    members.addActionListener(e -> showCard("Members", MembersView::create));
    navPanel.add(subjects);
    navPanel.add(timetable);
    navPanel.add(members);
    topBar.add(navPanel, BorderLayout.EAST);
    topBar.revalidate();
    topBar.repaint();
  }
}
