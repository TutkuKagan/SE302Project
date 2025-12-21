package com.example.scheduler.view;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.Exam;
import com.example.scheduler.model.Schedule;
import com.example.scheduler.model.StudentScheduleRow;
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
public class ScheduleByStudentView {

    private final ScheduleController controller;

    public ScheduleByStudentView(ScheduleController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        Schedule schedule = controller.getSchedule();

        if (schedule == null) {
            Tab tab = new Tab("By Student",
                    new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }

        TableView<StudentScheduleRow> table = new TableView<>();
        ObservableList<StudentScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            for (String studentId : exam.getCourse().getStudentIds()) {
                String courseCode = exam.getCourse().getCourseCode();
                int day = exam.getSlot().getDay();
                int slotIndex = exam.getSlot().getIndex();
                String timeRange = exam.getSlot().getTimeRange();
                String rooms = exam.getAssignedRooms().stream()
                        .map(Classroom::getRoomId)
                        .collect(Collectors.joining(","));

                items.add(new StudentScheduleRow(studentId, courseCode, day, slotIndex, timeRange, rooms));
            }
        }

        items.sort(Comparator
                .comparing(StudentScheduleRow::getStudentId)
                .thenComparingInt(StudentScheduleRow::getDay)
                .thenComparingInt(StudentScheduleRow::getSlotIndex));

        table.setItems(items);
        table.setPlaceholder(new Label("No schedule generated. Go to Actions > Generate Schedule."));

        TableColumn<StudentScheduleRow, String> stdCol = new TableColumn<>("Student ID");
        stdCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<StudentScheduleRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<StudentScheduleRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<StudentScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<StudentScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<StudentScheduleRow, String> roomCol = new TableColumn<>("Rooms");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("rooms"));

        table.getColumns().addAll(stdCol, courseCol, dayCol, slotCol, timeCol, roomCol);

        Tab tab = new Tab("By Student", table);
        tab.setClosable(false);
        return tab;
    }
}
