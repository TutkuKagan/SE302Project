package com.example.scheduler.view;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.model.Classroom;
import com.example.scheduler.model.DataRepository;
import com.example.scheduler.model.Exam;
import com.example.scheduler.model.Schedule;
import com.example.scheduler.model.StudentScheduleRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unchecked")
public class StudentScheduleView {

    private final ScheduleController controller;
    private final DataRepository repo;

    public StudentScheduleView(ScheduleController controller, DataRepository repo) {
        this.controller = controller;
        this.repo = repo;
    }

    public Tab createTab() {
        BorderPane root = new BorderPane();
        // choosing a student
        ComboBox<String> studentBox = new ComboBox<>();
        studentBox.setPromptText("Select student");
        List<String> studentIds = new ArrayList<>(repo.getStudents().keySet());

        studentIds.sort(Comparator.comparingInt(id -> {
            try {
                String numberPart = id.replaceAll("\\D+", "");
                return numberPart.isEmpty() ? 0 : Integer.parseInt(numberPart);
            } catch (NumberFormatException e) {
                return 0;
            }
        }));

        studentBox.getItems().addAll(studentIds);

        VBox top = new VBox(10);
        top.setPadding(new Insets(10));
        top.getChildren().addAll(
                new Label("Select a student:"),
                studentBox);
        root.setTop(top);

        // table
        TableView<StudentScheduleRow> table = new TableView<>();
        ObservableList<StudentScheduleRow> data = FXCollections.observableArrayList();
        table.setItems(data);
        table.setPlaceholder(new Label("No schedule generated. Go to Actions > Generate Schedule."));

        TableColumn<StudentScheduleRow, String> c1 = new TableColumn<>("Course");
        c1.setCellValueFactory(new PropertyValueFactory<>("courseCode"));
        c1.setPrefWidth(150);

        TableColumn<StudentScheduleRow, Integer> c2 = new TableColumn<>("Day");
        c2.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<StudentScheduleRow, Integer> c3 = new TableColumn<>("Slot");
        c3.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<StudentScheduleRow, String> c4 = new TableColumn<>("Time");
        c4.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<StudentScheduleRow, String> c5 = new TableColumn<>("Rooms");
        c5.setCellValueFactory(new PropertyValueFactory<>("rooms"));

        table.getColumns().addAll(c1, c2, c3, c4, c5);
        root.setCenter(table);

        // When student is chosen, fill the table
        studentBox.setOnAction(e -> {
            data.clear(); // clear the table first

            String studentId = studentBox.getValue();
            Schedule schedule = controller.getSchedule();
            if (studentId == null || schedule == null)
                return;

            for (Exam exam : schedule.getAllExams()) {
                // is this student registered to this course
                if (exam.getCourse().getStudentIds().contains(studentId)) {
                    String rooms = exam.getAssignedRooms()
                            .stream()
                            .map(Classroom::getRoomId)
                            .collect(Collectors.joining(","));

                    data.add(new StudentScheduleRow(
                            studentId,
                            exam.getCourse().getCourseCode(),
                            exam.getSlot().getDay(),
                            exam.getSlot().getIndex(),
                            exam.getSlot().getTimeRange(),
                            rooms));
                }
            }
        });

        Tab tab = new Tab("Student Schedule");
        tab.setClosable(false);
        tab.setContent(root);
        return tab;
    }
}
