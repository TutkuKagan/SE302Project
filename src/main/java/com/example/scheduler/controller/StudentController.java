package com.example.scheduler.controller;

import com.example.scheduler.model.DataRepository;
import com.example.scheduler.model.Student;
import com.example.scheduler.model.StudentRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;

public class StudentController {

    private final DataRepository repo;
    private final ObservableList<StudentRow> studentList;

    public StudentController(DataRepository repo) {
        this.repo = repo;
        this.studentList = FXCollections.observableArrayList();
        refreshList();
    }

    public ObservableList<StudentRow> getStudentList() {
        return studentList;
    }

    public void refreshList() {
        studentList.clear();
        for (Student s : repo.getStudents().values()) {
            studentList.add(new StudentRow(s.getStudentId()));
        }
        studentList.sort(Comparator.comparing(StudentRow::getStudentId));
    }

    public boolean addStudent(String id) {
        boolean added = repo.addStudent(id);
        if (added) {
            studentList.add(new StudentRow(id));
            studentList.sort(Comparator.comparing(StudentRow::getStudentId));
        }
        return added;
    }

    public boolean removeStudent(StudentRow row) {
        if (row == null)
            return false;
        boolean removed = repo.removeStudent(row.getStudentId());
        if (removed) {
            studentList.remove(row);
        }
        return removed;
    }
}
