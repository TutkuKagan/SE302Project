package com.example.scheduler.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.DataRepository;
import com.example.scheduler.model.Exam;
import com.example.scheduler.model.Schedule;
import com.example.scheduler.model.Student;

public class StudentScheduleService {

    private final DataRepository repo;

    public StudentScheduleService(DataRepository repo) {
        this.repo = repo;
    }

    public List<Exam> getScheduleForStudent(String studentId, Schedule schedule) {

        List<Exam> exams = new ArrayList<>();

        for (Exam e : schedule.getAllExams()) {
            if (e.getCourse().getStudentIds().contains(studentId)) {
                exams.add(e);
            }
        }

        exams.sort(Comparator
                .comparing((Exam e) -> e.getSlot().getDay())
                .thenComparing(e -> e.getSlot().getIndex()));

        return exams;
    }

    //
    public void printScheduleForStudent(String studentId, Schedule schedule) {

        Student student = repo.getStudents().get(studentId);
        if (student == null) {
            System.out.println("Student not found: " + studentId);
            return;
        }

        List<Exam> exams = getScheduleForStudent(studentId, schedule);

        System.out.println("Schedule for student " + studentId + ":");

        if (exams.isEmpty()) {
            System.out.println("  No exams.");
            return;
        }

        for (Exam e : exams) {
            String rooms = e.getAssignedRooms().stream()
                    .map(Classroom::getRoomId)
                    .collect(Collectors.joining(","));

            System.out.println(
                    "  Day " + e.getSlot().getDay() +
                            " | Slot " + e.getSlot().getIndex() +
                            " (" + e.getSlot().getTimeRange() + ")" +
                            " | Course: " + e.getCourse().getCourseCode() +
                            " | Room(s): " + rooms);
        }
    }
}
