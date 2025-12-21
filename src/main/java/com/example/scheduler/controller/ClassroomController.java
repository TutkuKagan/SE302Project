package com.example.scheduler.controller;

import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.ClassroomRow;
import com.example.scheduler.model.DataRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;

public class ClassroomController {

    private final DataRepository repo;
    private final ObservableList<ClassroomRow> classroomList;

    public ClassroomController(DataRepository repo) {
        this.repo = repo;
        this.classroomList = FXCollections.observableArrayList();
        refreshList();
    }

    public ObservableList<ClassroomRow> getClassroomList() {
        return classroomList;
    }

    public void refreshList() {
        classroomList.clear();
        for (Classroom room : repo.getClassrooms()) {
            classroomList.add(new ClassroomRow(room.getRoomId(), room.getCapacity()));
        }
        classroomList.sort(Comparator.comparing(ClassroomRow::getRoomId));
    }

    public boolean updateCapacity(ClassroomRow row, int newCapacity) {
        if (newCapacity <= 0)
            return false;
        boolean ok = repo.updateClassroomCapacity(row.getRoomId(), newCapacity);
        if (ok) {
            row.setCapacity(newCapacity);
        }
        return ok;
    }
}
