package com.example.scheduler.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class CourseScheduleRow {
    private final String courseCode;

    private final SimpleIntegerProperty day;
    private final SimpleIntegerProperty slotIndex;
    private final SimpleStringProperty timeRange;
    private final String rooms;
    private final int studentCount;

    public CourseScheduleRow(String courseCode, int day, int slotIndex,
            String timeRange, String rooms, int studentCount) {
        this.courseCode = courseCode;

        this.day = new SimpleIntegerProperty(day);
        this.slotIndex = new SimpleIntegerProperty(slotIndex);
        this.timeRange = new SimpleStringProperty(timeRange);
        this.rooms = rooms;
        this.studentCount = studentCount;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public int getDay() {
        return day.get();
    }

    public SimpleIntegerProperty dayProperty() {
        return day;
    }

    public void setDay(int newDay) {
        this.day.set(newDay);
    }

    public int getSlotIndex() {
        return slotIndex.get();
    }

    public SimpleIntegerProperty slotIndexProperty() {
        return slotIndex;
    }

    public void setSlotIndex(int newSlotIndex) {
        this.slotIndex.set(newSlotIndex);
    }

    public String getTimeRange() {
        return timeRange.get();
    }

    public SimpleStringProperty timeRangeProperty() {
        return timeRange;
    }

    public void setTimeRange(String newTimeRange) {
        this.timeRange.set(newTimeRange);
    }

    public String getRooms() {
        return rooms;
    }

    public int getStudentCount() {
        return studentCount;
    }
}
