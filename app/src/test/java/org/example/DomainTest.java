package org.example;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DomainTest {

    @Test
    void classContainsOneTeacherOfASubject() {
        Subject cs = new Subject("Computer Science HL");
        Teacher teacher = new Teacher("Shorena Abesadze");

        SchoolClass schoolClass = new SchoolClass(cs, teacher);
        schoolClass.setGrade("Grade 12");
        schoolClass.setRoom("3-15");
        schoolClass.setTime("9:00 AM - 9:45 AM");

        assertEquals(cs, schoolClass.getSubject());
        assertEquals(teacher, schoolClass.getTeacher());
        assertEquals("Grade 12", schoolClass.getGrade());
        assertEquals("3-15", schoolClass.getRoom());
        assertEquals("9:00 AM - 9:45 AM", schoolClass.getTime());
    }

    @Test
    void classRequiresSubjectAndTeacher() {
        assertThrows(IllegalArgumentException.class, () -> new SchoolClass(null, new Teacher("T")));
        assertThrows(IllegalArgumentException.class, () -> new SchoolClass(new Subject("Physics"), null));
    }

    @Test
    void membersTrackTheirCourses() {
        Subject math = new Subject("Mathematics");
        Subject physics = new Subject("Physics");

        Student student = new Student("Petre Babilua", "Grade 12");
        student.addCourse(math);
        student.addCourse(physics);
        assertEquals(2, student.getCourses().size());
        assertTrue(student.isLinkedTo(math));

        Teacher teacher = new Teacher("Tengiz Taktakishvili");
        teacher.addCourse(math);
        assertTrue(teacher.isLinkedTo(math));
        assertFalse(teacher.isLinkedTo(physics));
    }

    @Test
    void studentIsLimitedToSixCourses() {
        Student student = new Student("Ana Trapaidze", "Grade 12");
        RuntimeException thrown = assertThrows(IllegalStateException.class, () -> {
            for (int i = 1; i <= 7; i++) {
                student.addCourse(new Subject("Course " + i));
            }
        });
        assertTrue(thrown.getMessage().contains("6"));
    }

    @Test
    void scheduleIsRoundTripped() throws Exception {
        // persist -> parse -> same values
        java.io.File file = java.nio.file.Files.createTempFile("sched", ".xlsx").toFile();
        file.deleteOnExit();
        java.util.List<java.util.List<String>> data = new java.util.ArrayList<>();
        data.add(java.util.List.of("Time", "Subject", "Grade", "Teacher", "Room"));
        data.add(java.util.List.of("9:00 - 9:45", "Physics SL", "Grade 12", "Ana Trapaidze", "Lab"));
        ExcelReader.writeSchedule(file, data);

        java.util.List<java.util.List<String>> read = ExcelReader.readExcel(file);
        assertEquals("Physics SL", read.get(1).get(1));
        assertEquals("Ana Trapaidze", read.get(1).get(3));
    }
}
