package com.example.scheduler.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ClassroomRow {
    private final SimpleStringProperty roomId;
    private final SimpleIntegerProperty capacity;

    public ClassroomRow(String roomId, int capacity) {
        this.roomId = new SimpleStringProperty(roomId);
        this.capacity = new SimpleIntegerProperty(capacity);
    }

    public String getRoomId() {
        return roomId.get();
    }

    public SimpleStringProperty roomIdProperty() {
        return roomId;
    }

    public int getCapacity() {
        return capacity.get();
    }

    public SimpleIntegerProperty capacityProperty() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity.set(capacity);
    }
}
