package com.example.scheduler.view;

import com.example.scheduler.controller.StudentController;
import com.example.scheduler.model.StudentRow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings("deprecation")
public class StudentTabView {

    private final StudentController controller;

    public StudentTabView(StudentController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        BorderPane root = new BorderPane();

        TableView<StudentRow> table = new TableView<>();
        table.setItems(controller.getStudentList());

        TableColumn<StudentRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        table.getColumns().add(idCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField newStudentField = new TextField();
        newStudentField.setPromptText("New student ID (e.g. Std_ID_999)");

        Button addButton = new Button("➕ Add");
        addButton.setOnAction(e -> {
            String id = newStudentField.getText().trim();
            if (id.isEmpty()) {
                showError("Invalid ID", "Student ID can not be empty.");
                return;
            }
            boolean ok = controller.addStudent(id);
            if (!ok) {
                showError("Add Failed", "A student with this ID already exists.");
                return;
            }
            newStudentField.clear();
        });

        Button removeButton = new Button("🗑️Delete Selected");
        removeButton.setOnAction(e -> {
            StudentRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("No selection", "Please select a student to delete.");
                return;
            }
            boolean ok = controller.removeStudent(selected);
            if (!ok) {
                showError("Delete Failed", "Student could not be deleted.");
            }
        });

        Label info = new Label(
                "Note: Students are updated in memory; you must re-run scheduling to generate a new exam schedule.");
        info.setWrapText(true);

        HBox buttons = new HBox(10,
                new Label("New Student:"), newStudentField, addButton, removeButton);
        buttons.setPadding(new Insets(10));

        VBox bottom = new VBox(5, buttons, info);

        root.setCenter(table);
        root.setBottom(bottom);

        Tab tab = new Tab("Students", root);
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
