package com.example.scheduler.view;

import com.example.scheduler.controller.ClassroomController;
import com.example.scheduler.model.ClassroomRow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;

import javafx.scene.layout.VBox;
import javafx.util.converter.IntegerStringConverter;

@SuppressWarnings("deprecation")
public class ClassroomTabView {

    private final ClassroomController controller;

    public ClassroomTabView(ClassroomController controller) {
        this.controller = controller;
    }

    public Tab createTab() {

        TableView<ClassroomRow> table = new TableView<>();
        table.setItems(controller.getClassroomList());
        table.setEditable(true);

        TableColumn<ClassroomRow, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<ClassroomRow, Integer> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        capCol.setOnEditCommit(evt -> {
            ClassroomRow row = evt.getRowValue();
            Integer newCap = evt.getNewValue();
            if (newCap == null || newCap <= 0) {
                showError("Invalid Capacity", "Capacity must be a positive number.");
                table.refresh();
                return;
            }
            boolean ok = controller.updateCapacity(row, newCap);
            if (!ok) {
                showError("Update Failed.", "Capacity could not be updated.");
                table.refresh();
            }
        });

        table.getColumns().add(roomCol);
        table.getColumns().add(capCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label info = new Label(
                "Note: Classroom capacity changes are stored in memory; re-run scheduling if necessary.");
        info.setWrapText(true);

        VBox vbox = new VBox(10, table, info);
        vbox.setPadding(new Insets(10));

        Tab tab = new Tab("Classrooms", vbox);
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
