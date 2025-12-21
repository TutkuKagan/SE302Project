package com.example.scheduler.view;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.model.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;

import java.util.Comparator;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class ScheduleByCourseView {

    private final ScheduleController controller;

    public ScheduleByCourseView(ScheduleController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        Schedule schedule = controller.getSchedule();

        if (schedule == null) {
            Tab tab = new Tab("By Course",
                    new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }

        TableView<CourseScheduleRow> table = new TableView<>();
        table.setEditable(true);

        ObservableList<CourseScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            String courseCode = exam.getCourse().getCourseCode();
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String rooms = exam.getAssignedRooms().stream()
                    .map(Classroom::getRoomId)
                    .collect(Collectors.joining(","));
            int studentCount = exam.getCourse().getStudentCount();

            items.add(new CourseScheduleRow(courseCode, day, slotIndex, timeRange, rooms, studentCount));
        }

        items.sort(Comparator
                .comparingInt(CourseScheduleRow::getDay)
                .thenComparingInt(CourseScheduleRow::getSlotIndex)
                .thenComparing(CourseScheduleRow::getCourseCode));

        table.setItems(items);
        table.setPlaceholder(new Label("No schedule generated. Go to Actions > Generate Schedule."));

        TableColumn<CourseScheduleRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<CourseScheduleRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
        dayCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        dayCol.setOnEditCommit(event -> {
            CourseScheduleRow row = event.getRowValue();
            Integer newDay = event.getNewValue();

            if (newDay != null && newDay > 0) {
                row.setDay(newDay);
                applyRowToSchedule(row, table);
            } else {
                table.refresh();
            }
        });

        TableColumn<CourseScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));
        slotCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        slotCol.setOnEditCommit(event -> {
            CourseScheduleRow row = event.getRowValue();
            Integer newSlotIndex = event.getNewValue();

            if (newSlotIndex != null && newSlotIndex > 0) {
                row.setSlotIndex(newSlotIndex);
                applyRowToSchedule(row, table);
            } else {
                table.refresh();
            }
        });

        TableColumn<CourseScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<CourseScheduleRow, String> roomCol = new TableColumn<>("Rooms");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("rooms"));

        TableColumn<CourseScheduleRow, Integer> countCol = new TableColumn<>("Students");
        countCol.setCellValueFactory(new PropertyValueFactory<>("studentCount"));

        table.getColumns().addAll(courseCol, dayCol, slotCol, timeCol, roomCol, countCol);
        // table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); //
        // Deprecated

        Tab tab = new Tab("By Course", table);
        tab.setClosable(false);
        return tab;
    }

    private void applyRowToSchedule(CourseScheduleRow row, TableView<CourseScheduleRow> table) {
        Schedule schedule = controller.getSchedule();
        if (schedule == null)
            return;

        String courseCode = row.getCourseCode();
        Exam exam = schedule.getExamByCourse(courseCode);
        if (exam == null)
            return;

        int newDay = row.getDay();
        int newSlotIndex = row.getSlotIndex();

        // Use controller to find slot
        Slot newSlot = controller.findSlot(newDay, newSlotIndex);

        if (newSlot == null) {
            showError("Invalid Slot", "No slot found for Day " + newDay + ", Slot " + newSlotIndex + ".");
            // Revert
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            table.refresh();
            return;
        }

        // Validity checks using Controller
        if (controller.wouldCauseSameSlotStudentConflict(exam, newSlot)) {
            showError("Conflict", "Another exam with common students already exists in this slot (FR10 violation).");
            revertRow(row, exam, table);
            return;
        }

        if (controller.wouldCauseRoomConflict(exam, newSlot)) {
            showError("Conflict",
                    "One or more classrooms are already assigned to another exam in this slot (room conflict).");
            revertRow(row, exam, table);
            return;
        }

        if (controller.wouldViolateConsecutiveConstraint(exam, newSlot)) {
            showError("Constraint Violation", "This change creates consecutive exams for some students.");
            revertRow(row, exam, table);
            return;
        }

        if (controller.wouldViolateMaxTwoPerDayConstraint(exam, newSlot)) {
            showError("Constraint Violation",
                    "This change creates more than two exams in a single day for some students.");
            revertRow(row, exam, table);
            return;
        }

        boolean ok = controller.updateExamSlot(exam, newSlot, false);
        if (ok) {
            row.setTimeRange(newSlot.getTimeRange());
            // No need to updateAllViews here immediately since we are just inside this tab.
            // But ideally we should notify App?
            // For now, simple table refresh is enough for this view.
            table.refresh();
        }
    }

    private void revertRow(CourseScheduleRow row, Exam exam, TableView<CourseScheduleRow> table) {
        row.setDay(exam.getSlot().getDay());
        row.setSlotIndex(exam.getSlot().getIndex());
        table.refresh();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
