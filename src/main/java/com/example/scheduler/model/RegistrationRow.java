package com.example.scheduler.model;

import javafx.beans.property.SimpleStringProperty;

public class RegistrationRow {
    private final SimpleStringProperty studentId;
    private final SimpleStringProperty courseCode;

    public RegistrationRow(String studentId, String courseCode) {
        this.studentId = new SimpleStringProperty(studentId);
        this.courseCode = new SimpleStringProperty(courseCode);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public SimpleStringProperty studentIdProperty() {
        return studentId;
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public SimpleStringProperty courseCodeProperty() {
        return courseCode;
    }
}
