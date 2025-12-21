package com.example.scheduler.view;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.DaySlotScheduleRow;
import com.example.scheduler.model.Exam;
import com.example.scheduler.model.Schedule;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.Comparator;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ScheduleByDaySlotView {

    private final ScheduleController controller;

    public ScheduleByDaySlotView(ScheduleController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        Schedule schedule = controller.getSchedule();

        if (schedule == null) {
            Tab tab = new Tab("By Day/Slot",
                    new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }

        TableView<DaySlotScheduleRow> table = new TableView<>();
        ObservableList<DaySlotScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String courseCode = exam.getCourse().getCourseCode();
            String rooms = exam.getAssignedRooms().stream()
                    .map(Classroom::getRoomId)
                    .collect(Collectors.joining(","));
            int studentCount = exam.getCourse().getStudentCount();

            items.add(new DaySlotScheduleRow(day, slotIndex, timeRange, courseCode, rooms, studentCount));
        }

        items.sort(Comparator
                .comparingInt(DaySlotScheduleRow::getDay)
                .thenComparingInt(DaySlotScheduleRow::getSlotIndex)
                .thenComparing(DaySlotScheduleRow::getCourseCode));

        table.setItems(items);
        table.setPlaceholder(new Label("No schedule generated. Go to Actions > Generate Schedule."));

        TableColumn<DaySlotScheduleRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<DaySlotScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<DaySlotScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<DaySlotScheduleRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<DaySlotScheduleRow, String> roomCol = new TableColumn<>("Rooms");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("rooms"));

        TableColumn<DaySlotScheduleRow, Integer> countCol = new TableColumn<>("Students");
        countCol.setCellValueFactory(new PropertyValueFactory<>("studentCount"));

        table.getColumns().addAll(dayCol, slotCol, timeCol, courseCol, roomCol, countCol);

        Tab tab = new Tab("By Day/Slot", table);
        tab.setClosable(false);
        return tab;
    }
}
