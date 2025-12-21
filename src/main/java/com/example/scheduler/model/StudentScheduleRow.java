package com.example.scheduler.model;

public class StudentScheduleRow {
    private final String studentId;
    private final String courseCode;
    private final int day;
    private final int slotIndex;
    private final String timeRange;
    private final String rooms;

    public StudentScheduleRow(String studentId, String courseCode,
            int day, int slotIndex,
            String timeRange, String rooms) {
        this.studentId = studentId;
        this.courseCode = courseCode;
        this.day = day;
        this.slotIndex = slotIndex;
        this.timeRange = timeRange;
        this.rooms = rooms;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public int getDay() {
        return day;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getTimeRange() {
        return timeRange;
    }

    public String getRooms() {
        return rooms;
    }
}
