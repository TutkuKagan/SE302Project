package com.example.scheduler.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import com.example.scheduler.service.SlotGenerator;

public class DataRepository {

    private Map<String, Student> students = new HashMap<>();
    private Map<String, Course> courses = new HashMap<>();
    private List<Classroom> classrooms = new ArrayList<>();
    private List<Slot> slots = new ArrayList<>();

    public List<Slot> getSlots() {
        return slots;
    }

    public void setSlots(List<Slot> slots) {
        this.slots = slots;
    }

    public void loadAll(Path studentsCSV, Path coursesCSV,
            Path classroomsCSV, Path registrationsCSV) throws IOException {

        // Clear them first
        students.clear();
        courses.clear();
        classrooms.clear();

        // Students
        for (Student s : loadStudents(studentsCSV)) {
            students.put(s.getStudentId(), s);
        }

        // Courses
        for (Course c : loadCourses(coursesCSV)) {
            courses.put(c.getCourseCode(), c);
        }

        // Classrooms
        classrooms = loadClassrooms(classroomsCSV);

        // Registrations (in sampleData_AllAttendanceLists format)
        Map<String, Course> regMap = loadCourseRegistrations(registrationsCSV);

        // FR3
        // If there are courses that are registered but not in CSV,we add.
        for (String courseCode : regMap.keySet()) {
            courses.putIfAbsent(courseCode, new Course(courseCode));
            Course target = courses.get(courseCode);

            for (String stdId : regMap.get(courseCode).getStudentIds()) {
                target.addStudent(stdId);
            }
        }
    }

    public Map<String, Course> getCourses() {
        return courses;
    }

    public Map<String, Student> getStudents() {
        return students;
    }

    public List<Classroom> getClassrooms() {
        return classrooms;
    }

    // Returns all courses a given student is registered to.

    public List<Course> getCoursesOfStudent(String studentId) {
        List<Course> result = new ArrayList<>();

        for (Course c : courses.values()) {
            if (c.getStudentIds().contains(studentId)) {
                result.add(c);
            }
        }
        return result;
    }

    // Checks whether two courses conflict by sharing at least one student.

    public boolean coursesConflict(String courseA, String courseB) {
        Course c1 = courses.get(courseA);
        Course c2 = courses.get(courseB);

        if (c1 == null || c2 == null)
            return false;

        // Check if there is any shared student ID
        for (String s : c1.getStudentIds()) {
            if (c2.getStudentIds().contains(s)) {
                return true; // Conflict found
            }
        }
        return false; // No shared students
    }

    public List<Student> loadStudents(Path path) throws IOException {
        List<Student> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;

            if (first) {

                first = false;
                continue;
            }

            list.add(new Student(trimmed));
        }
        return list;
    }

    public List<Course> loadCourses(Path path) throws IOException {
        List<Course> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;

            if (first) {

                first = false;
                continue;
            }

            list.add(new Course(trimmed));
        }
        return list;
    }

    public List<Classroom> loadClassrooms(Path path) throws IOException {
        List<Classroom> list = new ArrayList<>();

        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        boolean first = true;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
                continue;

            if (first) {

                first = false;
                continue;
            }

            String[] parts = trimmed.split(";");
            if (parts.length < 2) {
                System.out.println("Invalid classroom entry: " + line);
                continue;
            }

            String room = parts[0].trim();
            int cap = Integer.parseInt(parts[1].trim());

            list.add(new Classroom(room, cap));
        }
        return list;
    }


    public Map<String, Course> loadCourseRegistrations(Path path) throws IOException {
        Map<String, Course> courseMap = new HashMap<>();
        // to have an easy control, we use BufferedReader
        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // skip the empty lines
                }

                // ex: "CourseCode_01"
                if (line.startsWith("CourseCode_")) {
                    String courseCode = line;

                    // To find the student list belongs to this course
                    String listLine = null;
                    while ((listLine = br.readLine()) != null) {
                        listLine = listLine.trim();
                        if (!listLine.isEmpty()) {
                            // list line
                            break;
                        }
                    }

                    if (listLine == null) {
                        // File is finished, exit
                        break;
                    }

                    // Ex:
                    // ['Std_ID_170', 'Std_ID_077', ..., 'Std_ID_168']
                    String cleaned = listLine
                            .replace("[", "")
                            .replace("]", "")
                            .replace("'", "")
                            .replace("\"", "");

                    String[] tokens = cleaned.split(",");
                    Course course = courseMap.computeIfAbsent(courseCode, Course::new);

                    for (String token : tokens) {
                        String id = token.trim();
                        if (id.isEmpty())
                            continue;
                        // Like now the id = "Std_ID_170"
                        course.addStudent(id);
                    }
                }
            }
        }

        return courseMap;
    }

    public void loadSlots(Path slotConfigCsv) throws IOException {
        List<String> lines = Files.readAllLines(slotConfigCsv, StandardCharsets.UTF_8);
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#"))
                continue;

            String[] parts = trimmed.split(";");
            int numDays = Integer.parseInt(parts[0].trim());

            List<String> timeRanges = new ArrayList<>();
            for (int i = 1; i < parts.length; i++) {
                timeRanges.add(parts[i].trim());
            }

            this.slots = SlotGenerator.generateSlots(numDays, timeRanges);
            break; // we assume that we used single line config
        }
    }

    // FR3
    // ------------------------------------------------------------------------------------------------------------------
    public boolean addStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty())
            return false;
        if (students.containsKey(studentId)) {
            return false;
        }
        students.put(studentId, new Student(studentId));
        return true;
    }

    public boolean removeStudent(String studentId) {
        if (!students.containsKey(studentId)) {
            return false;
        }
        students.remove(studentId);

        for (Course c : courses.values()) {
            c.getStudentIds().remove(studentId);
        }
        return true;
    }

    public boolean addCourse(String courseCode) {
        if (courseCode == null || courseCode.trim().isEmpty())
            return false;
        if (courses.containsKey(courseCode)) {
            return false;
        }
        courses.put(courseCode, new Course(courseCode));
        return true;
    }

    public boolean removeCourse(String courseCode) {
        if (!courses.containsKey(courseCode)) {
            return false;
        }
        courses.remove(courseCode);
        return true;
    }

    public boolean registerStudentToCourse(String studentId, String courseCode) {
        Student s = students.get(studentId);
        if (s == null) {

            return false;
        }

        Course c = courses.get(courseCode);
        if (c == null) {
            c = new Course(courseCode);
            courses.put(courseCode, c);
        }

        if (!c.getStudentIds().contains(studentId)) {
            c.addStudent(studentId);
            return true;
        }
        return false;
    }

    public boolean unregisterStudentFromCourse(String studentId, String courseCode) {
        Course c = courses.get(courseCode);
        if (c == null) {
            return false;
        }
        return c.getStudentIds().remove(studentId);
    }

    public boolean updateClassroomCapacity(String roomId, int newCapacity) {
        if (newCapacity <= 0)
            return false;

        for (Classroom room : classrooms) {
            if (room.getRoomId().equals(roomId)) {
                room.setCapacity(newCapacity);
                return true;
            }
        }
        return false;
    }
    // ------------------------------------------------------------------------------------------------------------------

}
