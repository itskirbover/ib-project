package org.example;

import java.util.Objects;

/** An IB DP course offered by the school. */
public class Subject {
  private final String name;
  private int id = -1;

  public Subject(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Subject name must not be null");
    }
    this.name = name.trim();
  }

  /** Database id; -1 until the entity has been loaded from or saved to the DB. */
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Subject other && other.name.equals(name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    return name;
  }
}
