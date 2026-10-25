1. Decomposing Problems

A. Database Management
Schema Definition: Design and configure a relational database containing tables for students, teachers, courses, and timetable slots.
Fields: Students, teachers (+ linked courses for both), courses, timetable slots
CRUD Operations: Create, read, update, delete.
B. Timetable Generation
Create a general scheduling algorithm able to identify errors.
Conflict Prevention Module: Logic checks implemented to enforce hard constraints (e.g. No student can be assigned to two classes simultaneously. No teacher can teach two courses in the same period or work during their marked unavailable slots. Teacher-allocated hours must not exceed their max hours per week.)
C. Interactive User Interface & Visualizer Mode
Input Forms: Drop-down menus (Combo Boxes) to select from the 6 DP course options for students, text fields for teacher constraints, and buttons to commit changes to MySQL or trigger the generation sequence.
Visual Renderer: A dynamic layout similar to a calendar to display generated or predetermined schedules.
Dynamic Conflict Handling: Clashing periods alter color properties (e.g. highlighting red) to alert coordinators immediately

2. Planning the Computational Thinking Process

Weeks 1–2: Environment Setup & Database Implementation

1. Planning: Identify all required tools and libraries.
2. Designing: N/A, as preparation only requires setup and verification of tools.
3. Developing: Install the Java Development Kit (JDK), MySQL, version control (Git/GitHub).
4. Testing/Evaluating: Verify installation, configuration and functionality of each tool.
   Weeks 3–4: Core GUI
5. Planning: Create flowcharts (or other diagrams) detailing how end users will interact with the program.
6. Designing: Sketch UI wireframes containing input text fields, choice selectors, and action buttons.
7. Developing: Build graphics using Swing (optionally implement basic functionality with MySQL here or in Weeks 5-7).
8. Testing/Evaluating: Perform user input tests to confirm the GUI is functoinal.
   Weeks 5–7: Generation
9. Planning: Create basic logic generating a schedule (e.g. check for conflicts for each student).
10. Designing: Create flowcharts and pseudocode for each problem.
11. Developing: Effectively the same process as step two, however now applied directly to Java+MySQL.
12. Testing: Create schedules with intentional conflicts to check if generation is functional.
13. Evaluating: Assess speed of execution and accuracy of the generated schedule.

Weeks 8–10: Visualizer

1. Planning: Plan the interactive updating mechanism for dragging or swapping grid allocations.
2. Designing: Sketch a wireframe of an interactive timetable, errors/conflicts, etc.
3. Developing: Program cell listener actions in Java; construct the interface highlighting engine to trigger when slots change.
4. Testing: Same testing as the core GUI and generation logic, but more accurate as the two have already been developed
5. Evaluating: Ensure scheduling changes require no unnecessary global regenerations.
   Weeks 11–12: Evaluation
6. Planning: Create a testing plan for each success criterion.
7. Testing: Create multiple procedurally generated test cases for each success criterion. Fix any bugs that come up (e.g. the program crashing due to invalid stored input) and repeat.
8. Evaluating: Compare the outcome of the final product against each success criterion.
