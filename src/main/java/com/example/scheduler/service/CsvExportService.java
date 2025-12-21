package com.example.scheduler.service;

import com.example.scheduler.model.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CsvExportService {

    private final DataRepository repo;

    public CsvExportService(DataRepository repo) {
        this.repo = repo;
    }

    // Schedule by Course
    public void exportByCourse(Schedule schedule, Path out) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("CourseCode;Day;SlotIndex;TimeRange;Rooms");

        for (Exam e : schedule.getAllExams()) {
            String rooms = e.getAssignedRooms().stream()
                    .map(Classroom::getRoomId)
                    .collect(Collectors.joining(","));

            Slot s = e.getSlot();
            lines.add(e.getCourse().getCourseCode() + ";" +
                    s.getDay() + ";" +
                    s.getIndex() + ";" +
                    s.getTimeRange() + ";" +
                    rooms);
        }

        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    // Schedule by Room
    public void exportByRoom(Schedule schedule, Path out) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("RoomId;Day;SlotIndex;TimeRange;CourseCode");

        for (Exam e : schedule.getAllExams()) {
            Slot s = e.getSlot();
            for (Classroom room : e.getAssignedRooms()) {
                lines.add(room.getRoomId() + ";" +
                        s.getDay() + ";" +
                        s.getIndex() + ";" +
                        s.getTimeRange() + ";" +
                        e.getCourse().getCourseCode());
            }
        }

        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    // Schedule by Student
    public void exportByStudent(Schedule schedule, Path out) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("StudentId;CourseCode;Day;SlotIndex;TimeRange;Rooms");

        for (Student student : repo.getStudents().values()) {
            String stdId = student.getStudentId();

            for (Exam e : schedule.getAllExams()) {
                if (e.getCourse().getStudentIds().contains(stdId)) {

                    Slot s = e.getSlot();
                    String rooms = e.getAssignedRooms().stream()
                            .map(Classroom::getRoomId)
                            .collect(Collectors.joining(","));

                    lines.add(stdId + ";" +
                            e.getCourse().getCourseCode() + ";" +
                            s.getDay() + ";" +
                            s.getIndex() + ";" +
                            s.getTimeRange() + ";" +
                            rooms);
                }
            }
        }

        Files.write(out, lines, StandardCharsets.UTF_8);
    }

    // Schedule by Day/Slot
    public void exportByDaySlot(Schedule schedule, Path out) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("Day;SlotIndex;TimeRange;RoomId;CourseCode");

        for (Slot slot : repo.getSlots()) {
            for (Exam e : schedule.getAllExams()) {
                if (e.getSlot().getDay() == slot.getDay() &&
                        e.getSlot().getIndex() == slot.getIndex()) {

                    for (Classroom room : e.getAssignedRooms()) {
                        lines.add(slot.getDay() + ";" +
                                slot.getIndex() + ";" +
                                slot.getTimeRange() + ";" +
                                room.getRoomId() + ";" +
                                e.getCourse().getCourseCode());
                    }
                }
            }
        }

        Files.write(out, lines, StandardCharsets.UTF_8);
    }
}
