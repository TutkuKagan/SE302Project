package com.example.scheduler.view;

import com.example.scheduler.controller.SlotController;
import com.example.scheduler.model.Schedule;
import com.example.scheduler.model.SlotConfigurationRow;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class SlotTabView {

    private final SlotController controller;
    private final Consumer<Schedule> onScheduleUpdate;
    private final Stage stage; // For FileChooser
    private Spinner<Integer> dayCountSpinner;

    public SlotTabView(SlotController controller, Stage stage, Consumer<Schedule> onScheduleUpdate) {
        this.controller = controller;
        this.stage = stage;
        this.onScheduleUpdate = onScheduleUpdate;
    }

    public Tab createTab() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));

        Label title = new Label("📅 Slot and Day Configuration");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        TableView<SlotConfigurationRow> slotTable = createSlotTable();

        // Load initial data
        controller.loadInitialData();
        int days = controller.calculateMaxDay();

        HBox controls = createSlotControls(days);

        mainLayout.getChildren().addAll(title, controls, slotTable);

        Tab tab = new Tab("Slot Config ", mainLayout);
        tab.setClosable(false);
        return tab;
    }

    private HBox createSlotControls(int initialDays) {
        HBox controls = new HBox(15);

        Label dayLabel = new Label("Total Exam Days:");
        this.dayCountSpinner = new Spinner<>(1, 10, initialDays);
        dayCountSpinner.setPrefWidth(70);

        Button addNewSlotButton = new Button("➕ Add New Slot");
        addNewSlotButton.setOnAction(e -> controller.addSlot());

        Button saveButton = new Button("Save Configuration");
        saveButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        saveButton.setOnAction(e -> handleSaveConfiguration());

        controls.getChildren().addAll(dayLabel, dayCountSpinner, addNewSlotButton, saveButton);
        return controls;
    }

    private TableView<SlotConfigurationRow> createSlotTable() {
        TableView<SlotConfigurationRow> table = new TableView<>();
        table.setEditable(true);
        table.setItems(controller.getSlotList());

        TableColumn<SlotConfigurationRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<SlotConfigurationRow, Integer> slotIndexCol = new TableColumn<>("Slot Index");
        slotIndexCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<SlotConfigurationRow, String> startTimeCol = new TableColumn<>("Start Time (HH:MM)");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        startTimeCol.setOnEditCommit(event -> event.getRowValue().setStartTime(event.getNewValue()));

        TableColumn<SlotConfigurationRow, String> endTimeCol = new TableColumn<>("End Time (HH:MM)");
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        endTimeCol.setOnEditCommit(event -> event.getRowValue().setEndTime(event.getNewValue()));

        TableColumn<SlotConfigurationRow, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<SlotConfigurationRow, Void>() {
            private final Button deleteButton = new Button("🗑️ Delete");
            {
                deleteButton.setOnAction(e -> {
                    SlotConfigurationRow row = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(row);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteButton);
            }
        });

        table.getColumns().add(dayCol);
        table.getColumns().add(slotIndexCol);
        table.getColumns().add(startTimeCol);
        table.getColumns().add(endTimeCol);
        table.getColumns().add(actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void handleSaveConfiguration() {
        int numDays = dayCountSpinner.getValue();

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Slot Configuration CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(stage);

        if (file == null)
            return;

        try {
            Schedule schedule = controller.saveConfiguration(file, numDays);

            String msg = "Slot configuration has been saved successfully.\n" +
                    "Number of days: " + numDays;
            if (schedule != null) {
                msg += "\n\nThe schedule was automatically regenerated.";
            } else {
                msg += "\n\nNote: Use Actions > Run / Re-run Scheduling to generate a schedule.";
            }
            showInfo("Saved", msg);

            if (onScheduleUpdate != null) {
                onScheduleUpdate.accept(schedule);
            }

        } catch (Exception e) {
            showError("Save Error", e.getMessage());
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
