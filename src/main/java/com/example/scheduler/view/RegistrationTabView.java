package com.example.scheduler.view;

import com.example.scheduler.controller.RegistrationController;
import com.example.scheduler.model.RegistrationRow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

@SuppressWarnings("deprecation")
public class RegistrationTabView {

    private final RegistrationController controller;

    public RegistrationTabView(RegistrationController controller) {
        this.controller = controller;
    }

    public Tab createTab() {
        BorderPane root = new BorderPane();

        ComboBox<String> studentCombo = new ComboBox<>();
        ComboBox<String> courseCombo = new ComboBox<>();

        studentCombo.setItems(controller.getStudentIds());
        courseCombo.setItems(controller.getCourseCodes());

        studentCombo.setPromptText("Select Student");
        courseCombo.setPromptText("Select Course");

        Button registerBtn = new Button("📌 Register");
        Button unregisterBtn = new Button("❌ Unregister");
        Button refreshBtn = new Button("🔄 Refresh");

        TableView<RegistrationRow> table = new TableView<>();
        table.setItems(controller.getRegistrationList());

        TableColumn<RegistrationRow, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<RegistrationRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        table.getColumns().add(studentCol);
        table.getColumns().add(courseCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        registerBtn.setOnAction(e -> {
            String sid = studentCombo.getValue();
            String code = courseCombo.getValue();
            if (sid == null || code == null) {
                showError("Missing Selection", "You must select both a student and a course.");
                return;
            }

            boolean ok = controller.register(sid, code);
            if (!ok) {
                showError("Registration Failed",
                        "Student not found or already registered to this course.");
            }
        });

        unregisterBtn.setOnAction(e -> {
            String sid = studentCombo.getValue();
            String code = courseCombo.getValue();
            if (sid == null || code == null) {
                showError("Missing Selection", "You must select both a student and a course.");
                return;
            }

            boolean ok = controller.unregister(sid, code);
            if (!ok) {
                showError("Delete Failed",
                        "This student does not appear to be registered for this course.");
            }
        });

        refreshBtn.setOnAction(e -> controller.refreshAll());

        HBox controls = new HBox(10,
                new Label("Student:"), studentCombo,
                new Label("Course:"), courseCombo,
                registerBtn, unregisterBtn, refreshBtn);
        controls.setPadding(new Insets(10));

        Label info = new Label(
                "Note: Registration changes are reflected in the student lists of Course objects and can be used in re-running the schedule.");
        info.setWrapText(true);

        VBox center = new VBox(10, table, info);
        center.setPadding(new Insets(10));

        root.setTop(controls);
        root.setCenter(center);

        Tab tab = new Tab("Registrations", root);
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
