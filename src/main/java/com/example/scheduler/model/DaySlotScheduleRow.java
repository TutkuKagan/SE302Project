package com.example.scheduler.model;

public class DaySlotScheduleRow {
    private final int day;
    private final int slotIndex;
    private final String timeRange;
    private final String courseCode;
    private final String rooms;
    private final int studentCount;

    public DaySlotScheduleRow(int day, int slotIndex,
            String timeRange, String courseCode, String rooms, int studentCount) {
        this.day = day;
        this.slotIndex = slotIndex;
        this.timeRange = timeRange;
        this.courseCode = courseCode;
        this.rooms = rooms;
        this.studentCount = studentCount;
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

    public String getCourseCode() {
        return courseCode;
    }

    public String getRooms() {
        return rooms;
    }

    public int getStudentCount() {
        return studentCount;
    }

}
