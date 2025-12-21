package com.example.scheduler.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CourseRow {
    private final SimpleStringProperty courseCode;
    private final SimpleIntegerProperty studentCount;

    public CourseRow(String courseCode, int studentCount) {
        this.courseCode = new SimpleStringProperty(courseCode);
        this.studentCount = new SimpleIntegerProperty(studentCount);
    }

    public String getCourseCode() {
        return courseCode.get();
    }

    public SimpleStringProperty courseCodeProperty() {
        return courseCode;
    }

    public int getStudentCount() {
        return studentCount.get();
    }

    public SimpleIntegerProperty studentCountProperty() {
        return studentCount;
    }

    public void setStudentCount(int count) {
        this.studentCount.set(count);
    }
}
