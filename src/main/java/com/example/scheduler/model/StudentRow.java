package com.example.scheduler.model;

import javafx.beans.property.SimpleStringProperty;

public class StudentRow {
    private final SimpleStringProperty studentId;

    public StudentRow(String studentId) {
        this.studentId = new SimpleStringProperty(studentId);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public SimpleStringProperty studentIdProperty() {
        return studentId;
    }
}
