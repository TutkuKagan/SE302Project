package com.example.scheduler.model;

public class RoomScheduleRow {
    private final String roomId;
    private final int day;
    private final int slotIndex;
    private final String timeRange;
    private final String courseCode;

    public RoomScheduleRow(String roomId, int day, int slotIndex,
            String timeRange, String courseCode) {
        this.roomId = roomId;
        this.day = day;
        this.slotIndex = slotIndex;
        this.timeRange = timeRange;
        this.courseCode = courseCode;
    }

    public String getRoomId() {
        return roomId;
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
}
