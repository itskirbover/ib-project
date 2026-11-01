package org.example;

import java.util.List;

/**
 * The full IB Diploma Programme subject catalog (groups 1-6), seeded on first
 * run. Every subject that exists at both levels is listed twice, as an HL and
 * an SL class; ab initio languages are only offered at SL, so they stay
 * undivided.
 */
public final class IBSubjects {
  public static final List<String> ALL = List.of(
      // Group 1: Studies in language and literature
      "English A: Literature HL",
      "English A: Literature SL",
      "English A: Language and Literature HL",
      "English A: Language and Literature SL",
      "Georgian A: Literature HL",
      "Georgian A: Literature SL",
      "Russian A: Literature HL",
      "Russian A: Literature SL",
      // Group 2: Language acquisition
      "English B HL",
      "English B SL",
      "Spanish B HL",
      "Spanish B SL",
      "Spanish ab initio",
      "German B HL",
      "German B SL",
      "German ab initio",
      "French B HL",
      "French B SL",
      "French ab initio",
      "Mandarin ab initio",
      // Group 3: Individuals and societies
      "History HL",
      "History SL",
      "Geography HL",
      "Geography SL",
      "Economics HL",
      "Economics SL",
      "Business Management HL",
      "Business Management SL",
      "Global Politics HL",
      "Global Politics SL",
      "Psychology HL",
      "Psychology SL",
      "Philosophy HL",
      "Philosophy SL",
      "Social and Cultural Anthropology HL",
      "Social and Cultural Anthropology SL",
      "World Religions HL",
      "World Religions SL",
      // Group 4: Sciences
      "Biology HL",
      "Biology SL",
      "Chemistry HL",
      "Chemistry SL",
      "Physics HL",
      "Physics SL",
      "Computer Science HL",
      "Computer Science SL",
      "Design Technology HL",
      "Design Technology SL",
      "Sports, Exercise and Health Science HL",
      "Sports, Exercise and Health Science SL",
      "Environmental Systems and Societies HL",
      "Environmental Systems and Societies SL",
      // Group 5: Mathematics
      "Mathematics: Analysis and Approaches HL",
      "Mathematics: Analysis and Approaches SL",
      "Mathematics: Applications and Interpretation HL",
      "Mathematics: Applications and Interpretation SL",
      // Group 6: The arts
      "Music HL",
      "Music SL",
      "Theatre HL",
      "Theatre SL",
      "Visual Arts HL",
      "Visual Arts SL",
      "Film HL",
      "Film SL",
      "Dance HL",
      "Dance SL");

  private IBSubjects() {
  }
}
