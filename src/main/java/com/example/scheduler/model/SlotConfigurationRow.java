package com.example.scheduler.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SlotConfigurationRow {
    private final SimpleIntegerProperty day;
    private final SimpleIntegerProperty slotIndex;
    private final SimpleStringProperty startTime;
    private final SimpleStringProperty endTime;

    public SlotConfigurationRow(int day, int slotIndex, String startTime, String endTime) {
        this.day = new SimpleIntegerProperty(day);
        this.slotIndex = new SimpleIntegerProperty(slotIndex);
        this.startTime = new SimpleStringProperty(startTime);
        this.endTime = new SimpleStringProperty(endTime);
    }

    public int getDay() {
        return day.get();
    }

    public SimpleIntegerProperty dayProperty() {
        return day;
    }

    public void setDay(int day) {
        this.day.set(day);
    }

    public int getSlotIndex() {
        return slotIndex.get();
    }

    public SimpleIntegerProperty slotIndexProperty() {
        return slotIndex;
    }

    public String getStartTime() {
        return startTime.get();
    }

    public SimpleStringProperty startTimeProperty() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime.set(startTime);
    }

    public String getEndTime() {
        return endTime.get();
    }

    public SimpleStringProperty endTimeProperty() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime.set(endTime);
    }
}
