Problem Specification

I attend an IB World School which tracks which 6 DP courses their students take by manually updating entries every time new students arrive and/or every time that a course is changed. Because the school creates a timetable of classes based on this document by hand, this poses the school a major inconvenience and stress for those who manage the students timetables. Due to the large amount of students, creating so many timetables at once and balancing out the needs of every student and teacher makes the schedule extremely prone to errors.
I have decided to create a software solution that may fix this issue for any school facing it. This software should be able to allow for coordinators to input and visualize which courses are offered by the IB World School, together with information about students and teachers, including which courses they attend or teach. This data would be stored persistently, and then be used to create a timetable without clashes.
After the timetable is created, the software will have a visualizer mode, which allows for the user to then configure the timetable as much as they need without unnecessary regeneration of the schedule.

Computational Context

The proposed solution will be developed as a standalone desktop application utilizing the Java programming language. Java is the most appropriate choice for this project due to it being an object-oriented programming language, which makes it much easier to store students, teachers, classes and etc. Java also has the benefit of platform-independence using the Java Virtual Machine (JVM). This ensures that the school’s administration can run the software on various operating systems, and makes the program easily scalable to any computer.
To store data persistently, I have decided to use the database management system MySQL. This is firstly because MySQL works easily with Java using its standard libraries, and secondly because of its easy scalability and performance when dealing with larger databases, such as hundreds of students and teachers in one database.

Success Criteria

The app will allow the user to input basic, data about students, classes and teachers. This includes how many classes a student is attending, how many times a teacher can teach a course per week, and areas where a teacher is not able to teach.
The app will allow a user to state which 6 IB DP courses a student takes, and which courses a teacher is teaching.
The app will be able to save this data persistently.
The app will be able to automatically create a timetable of classes based on given data.
The timetable will not feature clashes of two classes.
The app will have a visualizer mode which shows a timetable generated within the app or by the user, which allows the user to edit the timetable interactively.
The visualizer mode will be able to highlight any issues or clashes with the schedule dynamically.
