package com.example.scheduler.view;

import com.example.scheduler.controller.CourseController;
import com.example.scheduler.model.CourseRow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings("deprecation")
public class CourseTabView {

    private final CourseController controller;

    public CourseTabView(CourseController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        BorderPane root = new BorderPane();

        TableView<CourseRow> table = new TableView<>();
        table.setItems(controller.getCourseList());

        TableColumn<CourseRow, String> codeCol = new TableColumn<>("Course Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<CourseRow, Integer> countCol = new TableColumn<>("Student Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("studentCount"));

        table.getColumns().add(codeCol);
        table.getColumns().add(countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField newCourseField = new TextField();
        newCourseField.setPromptText("New course code (e.g. CourseCode_99)");

        Button addButton = new Button("➕ Add Course");
        addButton.setOnAction(e -> {
            String code = newCourseField.getText().trim();
            if (code.isEmpty()) {
                showError("Invalid Course Code", "Course code cannot be empty.");
                return;
            }
            boolean ok = controller.addCourse(code, 0);
            if (!ok) {
                showError("Add Failed", "A course with this code already exists.");
                return;
            }
            newCourseField.clear();
        });

        Button removeButton = new Button("🗑️ Delete selected");
        removeButton.setOnAction(e -> {
            CourseRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("No Selection", "Please select a course to delete.");
                return;
            }
            boolean ok = controller.removeCourse(selected);
            if (!ok) {
                showError("Delete Failed", "Course could not be deleted.");
            }
        });

        Label info = new Label(
                "Note: Courses are updated in memory; re-run scheduling to generate a new exam schedule.");
        info.setWrapText(true);

        HBox buttons = new HBox(10,
                new Label("New Course:"), newCourseField, addButton, removeButton);
        buttons.setPadding(new Insets(10));

        VBox bottom = new VBox(5, buttons, info);

        root.setCenter(table);
        root.setBottom(bottom);

        Tab tab = new Tab("Courses", root);
        tab.setClosable(false);
        return tab;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
