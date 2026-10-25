package org.example;

import java.util.Objects;

/** An IB DP course offered by the school. */
public class Subject {
  private final String name;

  public Subject(String name) {
    if (name == null) {
      throw new IllegalArgumentException("Subject name must not be null");
    }
    this.name = name.trim();
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
