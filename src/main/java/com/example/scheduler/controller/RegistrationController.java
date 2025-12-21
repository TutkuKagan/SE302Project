package com.example.scheduler.controller;

import com.example.scheduler.model.Course;
import com.example.scheduler.model.DataRepository;
import com.example.scheduler.model.RegistrationRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class RegistrationController {

    private final DataRepository repo;
    private final ObservableList<RegistrationRow> registrationList;
    private final ObservableList<String> studentIds;
    private final ObservableList<String> courseCodes;

    public RegistrationController(DataRepository repo) {
        this.repo = repo;
        this.registrationList = FXCollections.observableArrayList();
        this.studentIds = FXCollections.observableArrayList();
        this.courseCodes = FXCollections.observableArrayList();
        refreshAll();
    }

    public ObservableList<RegistrationRow> getRegistrationList() {
        return registrationList;
    }

    public ObservableList<String> getStudentIds() {
        return studentIds;
    }

    public ObservableList<String> getCourseCodes() {
        return courseCodes;
    }

    public void refreshAll() {
        // Students
        List<String> sIds = new ArrayList<>(repo.getStudents().keySet());
        Collections.sort(sIds);
        studentIds.setAll(sIds);

        // Courses
        List<String> cIds = new ArrayList<>(repo.getCourses().keySet());
        Collections.sort(cIds);
        courseCodes.setAll(cIds);

        // Registrations
        registrationList.clear();
        for (Course c : repo.getCourses().values()) {
            for (String sid : c.getStudentIds()) {
                registrationList.add(new RegistrationRow(sid, c.getCourseCode()));
            }
        }
        registrationList.sort(Comparator
                .comparing(RegistrationRow::getCourseCode)
                .thenComparing(RegistrationRow::getStudentId));
    }

    public boolean register(String studentId, String courseCode) {
        if (studentId == null || courseCode == null)
            return false;
        boolean ok = repo.registerStudentToCourse(studentId, courseCode);
        if (ok) {
            registrationList.add(new RegistrationRow(studentId, courseCode));
            registrationList.sort(Comparator
                    .comparing(RegistrationRow::getCourseCode)
                    .thenComparing(RegistrationRow::getStudentId));
        }
        return ok;
    }

    public boolean unregister(String studentId, String courseCode) {
        if (studentId == null || courseCode == null)
            return false;

        boolean ok = repo.unregisterStudentFromCourse(studentId, courseCode);
        if (ok) {
            registrationList.removeIf(r -> r.getStudentId().equals(studentId) && r.getCourseCode().equals(courseCode));
        }
        return ok;
    }
}
