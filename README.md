# ib-project

A standalone desktop application for IB World Schools to manage DP course selections and generate clash-free student/teacher timetables.

Written in Java as an IB Computer Science internal assessment solution (see `criterionA.md` / `criterionB.md`).

## Features

- Input and manage students, teachers, and the IB DP courses they take/teach
- Persistent storage in an embedded MySQL-compatible database (MariaDB4j)
- Automatic timetable generation without class clashes
- Visualizer mode for interactively editing generated timetables
- Dynamic highlighting of clashes and scheduling issues
- Excel import/export via Apache POI

## Requirements

- JDK 21 (the Gradle toolchain can download one automatically via the foojay resolver)
- Linux (the embedded MariaDB binaries bundled are `linux64`)

## Building and running

```bash
./gradlew run        # build and launch the application
./gradlew build      # build, run checks and tests
./gradlew test       # run the JUnit test suite
```

## Project structure

```
app/src/main/java/org/example/
├── App.java            # Entry point
├── MainShell.java      # Main window / navigation shell
├── MembersView.java    # Student & teacher management
├── SubjectView.java    # IB subject management
├── TimetableView.java  # Timetable generation & editing
├── *CalendarView.java  # Calendar visualizers (member/subject)
├── ConflictDetector.java / ConflictSolver.java / TimeSuggester.java
├── *Dao.java           # Database access layer
├── Database.java / Schema.java
├── ExcelReader.java / ExcelWriter.java
└── Palette.java / Aa*.java / Views.java   # UI components
```

## Tech stack

- Java 21, Swing (custom AWT-based UI components)
- Gradle 9 (Kotlin DSL) with the `application` plugin
- MariaDB4j (embedded MariaDB server) + MariaDB JDBC client
- Apache POI for spreadsheet I/O
- JUnit Jupiter for testing
