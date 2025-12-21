package com.example.scheduler.controller;

import com.example.scheduler.model.*;
import com.example.scheduler.service.CsvExportService;
import com.example.scheduler.service.SchedulingEngine;

import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashSet;

import java.util.Set;

public class ScheduleController {

    private final DataRepository repo;
    private final CsvExportService exportService;
    private Schedule schedule;

    public ScheduleController(DataRepository repo) {
        this.repo = repo;
        this.exportService = new CsvExportService(repo);
    }

    public Schedule getSchedule() {
        return schedule;
    }

    public void generateSchedule() {
        if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
            this.schedule = new SchedulingEngine(repo).generateExamSchedule();
        } else {
            this.schedule = null;
        }
    }

    public void setSchedule(Schedule schedule) {
        this.schedule = schedule;
    }

    public Slot findSlot(int day, int index) {
        for (Slot s : repo.getSlots()) {
            if (s.getDay() == day && s.getIndex() == index) {
                return s;
            }
        }
        return null;
    }

    public void exportSchedule(Stage owner, String type) {
        if (schedule == null) {
            showError("No Schedule", "Please generate a schedule first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export " + type);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(owner);
        if (file == null)
            return;

        try {
            switch (type) {
                case "By Course":
                    exportService.exportByCourse(schedule, file.toPath());
                    break;
                case "By Room":
                    exportService.exportByRoom(schedule, file.toPath());
                    break;
                case "By Student":
                    exportService.exportByStudent(schedule, file.toPath());
                    break;
                case "By Day/Slot":
                    exportService.exportByDaySlot(schedule, file.toPath());
                    break;
            }
            showInfo("Export Successful", "Schedule exported to " + file.getName());
        } catch (Exception e) {
            showError("Export Failed", "Could not save file: " + e.getMessage());
        }
    }

    // --- Constraint Checking Logic (Moved from App) ---

    public boolean updateExamSlot(Exam exam, Slot newSlot, boolean force) {
        if (exam == null || newSlot == null)
            return false;

        // If not forcing, check constraints?
        // Logic for UI usually checks constraints BEFORE calling this, or this returns
        // validation result?
        // Let's assume validation is done by caller using public wouldViolate methods.

        exam.setSlot(newSlot);
        return true;
    }

    public boolean wouldCauseSameSlotStudentConflict(Exam movingExam, Slot newSlot) {
        Set<String> movingStudents = new HashSet<>(movingExam.getCourse().getStudentIds());

        for (Exam other : schedule.getAllExams()) {
            if (other == movingExam)
                continue;

            if (other.getSlot().getDay() == newSlot.getDay()
                    && other.getSlot().getIndex() == newSlot.getIndex()) {

                for (String s : movingStudents) {
                    if (other.getCourse().getStudentIds().contains(s)) {
                        return true; // Conflict
                    }
                }
            }
        }
        return false;
    }

    public boolean wouldCauseRoomConflict(Exam movingExam, Slot newSlot) {
        Set<String> movingRooms = new HashSet<>();
        for (Classroom cr : movingExam.getAssignedRooms()) {
            movingRooms.add(cr.getRoomId());
        }

        for (Exam other : schedule.getAllExams()) {
            if (other == movingExam)
                continue;

            if (other.getSlot().getDay() == newSlot.getDay()
                    && other.getSlot().getIndex() == newSlot.getIndex()) {

                for (Classroom cr : other.getAssignedRooms()) {
                    if (movingRooms.contains(cr.getRoomId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean wouldViolateConsecutiveConstraint(Exam movingExam, Slot newSlot) {
        int day = newSlot.getDay();
        int idx = newSlot.getIndex();
        int prev = idx - 1;
        int next = idx + 1;

        for (String studentId : movingExam.getCourse().getStudentIds()) {
            for (Exam other : schedule.getAllExams()) {
                if (other == movingExam)
                    continue;
                if (!other.getCourse().getStudentIds().contains(studentId))
                    continue;
                if (other.getSlot().getDay() != day)
                    continue;

                int oIdx = other.getSlot().getIndex();
                if (oIdx == prev || oIdx == next) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean wouldViolateMaxTwoPerDayConstraint(Exam movingExam, Slot newSlot) {
        int day = newSlot.getDay();

        for (String studentId : movingExam.getCourse().getStudentIds()) {
            int count = 0;
            for (Exam other : schedule.getAllExams()) {
                if (other == movingExam)
                    continue;
                if (!other.getCourse().getStudentIds().contains(studentId))
                    continue;
                if (other.getSlot().getDay() == day) {
                    count++;
                    if (count >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // --- Helpers ---

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
