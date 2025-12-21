package com.example.scheduler.view;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.Exam;
import com.example.scheduler.model.RoomScheduleRow;
import com.example.scheduler.model.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;

@SuppressWarnings("unchecked")
public class ScheduleByRoomView {

    private final ScheduleController controller;

    public ScheduleByRoomView(ScheduleController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        Schedule schedule = controller.getSchedule();

        if (schedule == null) {
            Tab tab = new Tab("By Room",
                    new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }

        TableView<RoomScheduleRow> table = new TableView<>();
        ObservableList<RoomScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String courseCode = exam.getCourse().getCourseCode();

            for (Classroom room : exam.getAssignedRooms()) {
                items.add(new RoomScheduleRow(room.getRoomId(), day, slotIndex, timeRange, courseCode));
            }
        }

        items.sort(Comparator
                .comparing(RoomScheduleRow::getRoomId)
                .thenComparingInt(RoomScheduleRow::getDay)
                .thenComparingInt(RoomScheduleRow::getSlotIndex));

        table.setItems(items);
        table.setPlaceholder(new Label("No schedule generated. Go to Actions > Generate Schedule."));

        TableColumn<RoomScheduleRow, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<RoomScheduleRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<RoomScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<RoomScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<RoomScheduleRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        table.getColumns().addAll(roomCol, dayCol, slotCol, timeCol, courseCol);

        Tab tab = new Tab("By Room", table);
        tab.setClosable(false);
        return tab;
    }
}
