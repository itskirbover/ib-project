package org.example;

import java.util.List;

/** The full IB Diploma Programme subject catalog (groups 1-6), seeded on first run. */
public final class IbSubjects {
  public static final List<String> ALL = List.of(
      // Group 1: Studies in language and literature
      "English A: Literature",
      "English A: Language and Literature",
      "Georgian A: Literature",
      "Russian A: Literature",
      // Group 2: Language acquisition
      "English B",
      "Spanish ab initio",
      "Spanish B",
      "German ab initio",
      "German B",
      "French ab initio",
      "French B",
      "Mandarin ab initio",
      // Group 3: Individuals and societies
      "History",
      "Geography",
      "Economics",
      "Business Management",
      "Global Politics",
      "Psychology",
      "Philosophy",
      "Social and Cultural Anthropology",
      "World Religions",
      // Group 4: Sciences
      "Biology",
      "Chemistry",
      "Physics",
      "Computer Science",
      "Design Technology",
      "Sports, Exercise and Health Science",
      "Environmental Systems and Societies",
      // Group 5: Mathematics
      "Mathematics: Analysis and Approaches",
      "Mathematics: Applications and Interpretation",
      // Group 6: The arts
      "Music",
      "Theatre",
      "Visual Arts",
      "Film",
      "Dance");

  private IbSubjects() {
  }
}
