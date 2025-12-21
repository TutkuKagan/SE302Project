
package com.example.scheduler;

import javafx.application.Application;

import javafx.scene.Scene;
import javafx.scene.control.*;

import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;
import javafx.scene.control.Alert.AlertType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.io.IOException;
import java.util.*;

import javafx.geometry.Insets;

import com.example.scheduler.model.*;
import com.example.scheduler.service.*;
import com.example.scheduler.controller.StudentController;
import com.example.scheduler.view.StudentTabView;
import com.example.scheduler.controller.CourseController;
import com.example.scheduler.view.CourseTabView;
import com.example.scheduler.controller.ClassroomController;
import com.example.scheduler.view.ClassroomTabView;
import com.example.scheduler.controller.SlotController;
import com.example.scheduler.view.SlotTabView;
import com.example.scheduler.controller.RegistrationController;
import com.example.scheduler.view.RegistrationTabView;

import com.example.scheduler.controller.ScheduleController;
import com.example.scheduler.view.ScheduleByCourseView;
import com.example.scheduler.view.ScheduleByRoomView;
import com.example.scheduler.view.ScheduleByStudentView;
import com.example.scheduler.view.ScheduleByDaySlotView;
import com.example.scheduler.view.StudentScheduleView;

public class ExamSchedulerApp extends Application {

    private Stage primaryStage;

    private DataRepository repo;
    private CsvImportService importService;
    private CsvExportService exportService;
    private StudentController studentController;
    private CourseController courseController;
    private ClassroomController classroomController;
    private SlotController slotController;
    private RegistrationController registrationController;
    private ScheduleController scheduleController;

    // Last imported files (optional, for re-import/re-run)
    private Path studentsPath;
    private Path coursesPath;
    private Path classroomsPath;
    private Path registrationsPath;
    private Path slotConfigPath;

    private Schedule schedule;

    private Label statusLabel;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        repo = new DataRepository();
        this.importService = new CsvImportService(repo);
        this.exportService = new CsvExportService(repo);
        this.studentController = new StudentController(repo);
        this.courseController = new CourseController(repo);
        this.classroomController = new ClassroomController(repo);
        this.slotController = new SlotController(repo);
        this.registrationController = new RegistrationController(repo);
        this.scheduleController = new ScheduleController(repo);

        statusLabel = new Label("Ready");
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-border-color: #ccc; -fx-background-color: #eee; -fx-pref-width: 10000;");

        // Try to load sample data from the working directory (optional).
        tryAutoLoadDefaultSampleData();

        // If we already have data, try to schedule once.
        if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
            try {
                scheduleController.generateSchedule();
                this.schedule = scheduleController.getSchedule();
                statusLabel.setText("Schedule generated from default data.");
            } catch (RuntimeException ex) {
                this.schedule = null;
                showError("Scheduling Error",
                        "No feasible schedule could be generated with current data\n" + ex.getMessage());
            }
        }

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createDataManagementTab());
        tabPane.getTabs().add(createSlotConfigurationTab());
        tabPane.getTabs().add(new ScheduleByCourseView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByRoomView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByStudentView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByDaySlotView(scheduleController).createTab());
        tabPane.getTabs().add(new StudentScheduleView(scheduleController, repo).createTab());

        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(tabPane);
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 1100, 650);
        primaryStage.setTitle("Desktop Exam Scheduling Assistant - Schedule Views");
        primaryStage.setScene(scene);
        primaryStage.show();

    }

    private void tryAutoLoadDefaultSampleData() {
        try {
            Path s = Paths.get("sampleData_AllStudents.csv");
            Path c = Paths.get("sampleData_AllCourses.csv");
            Path r = Paths.get("sampleData_AllClassroomsAndTheirCapacities.csv");
            Path a = Paths.get("sampleData_AllAttendanceLists.csv");
            Path slots = Paths.get("sampleData_slot_config.csv");

            if (Files.exists(s) && Files.exists(c) && Files.exists(r) && Files.exists(a) && Files.exists(slots)) {
                this.studentsPath = s;
                this.coursesPath = c;
                this.classroomsPath = r;
                this.registrationsPath = a;
                this.slotConfigPath = slots;

                importService.importAll(s, c, r, a, slots);
                refreshAllDataControllers();
                scheduleController.generateSchedule();
                this.schedule = scheduleController.getSchedule();
            }
        } catch (IOException e) {
            showError("CSV Import Error",
                    "An error occurred while importing default CSV files\n" + e.getMessage());
        }
    }

    private void refreshAllDataControllers() {
        if (studentController != null)
            studentController.refreshList();
        if (courseController != null)
            courseController.refreshList();
        if (classroomController != null)
            classroomController.refreshList();
        if (registrationController != null)
            registrationController.refreshAll();
    }

    private Tab createDataManagementTab() {
        TabPane inner = new TabPane();
        inner.getTabs().add(createStudentManagementTab());
        inner.getTabs().add(createCourseManagementTab());
        inner.getTabs().add(createRegistrationManagementTab());
        inner.getTabs().add(createClassroomManagementTab());

        Tab outer = new Tab("Data Management ", inner);
        outer.setClosable(false);
        return outer;
    }

    private MenuBar createMenuBar() {
        boolean hasSchedule = (schedule != null);

        Menu fileMenu = new Menu("File");

        MenuItem importAll = new MenuItem("📥 Import CSV Files...");
        importAll.setOnAction(e -> handleImportAll());

        MenuItem importSlotsOnly = new MenuItem("📥 Import Slot Configuration Only...");
        importSlotsOnly.setOnAction(e -> handleImportSlotsOnly());

        MenuItem exit = new MenuItem("Exit");
        exit.setOnAction(e -> {
            if (primaryStage != null)
                primaryStage.close();
        });

        fileMenu.getItems().addAll(importAll, importSlotsOnly, new SeparatorMenuItem(), exit);

        Menu exportMenu = new Menu("Export");

        MenuItem exportByCourse = new MenuItem("Export – By Course");
        exportByCourse.setOnAction(e -> handleExportByCourse());
        exportByCourse.setDisable(!hasSchedule);

        MenuItem exportByRoom = new MenuItem("Export – By Room");
        exportByRoom.setOnAction(e -> handleExportByRoom());
        exportByRoom.setDisable(!hasSchedule);

        MenuItem exportByStudent = new MenuItem("Export – By Student");
        exportByStudent.setOnAction(e -> handleExportByStudent());
        exportByStudent.setDisable(!hasSchedule);

        MenuItem exportByDaySlot = new MenuItem("Export – By Day/Slot");
        exportByDaySlot.setOnAction(e -> handleExportByDaySlot());
        exportByDaySlot.setDisable(!hasSchedule);

        exportMenu.getItems().addAll(exportByCourse, exportByRoom, exportByStudent, exportByDaySlot);

        Menu actionsMenu = new Menu("Actions");
        MenuItem runItem = new MenuItem("▶ Run / Re-run Scheduling");
        runItem.setOnAction(e -> handleReRunScheduling());
        actionsMenu.getItems().add(runItem);

        Menu helpMenu = new Menu("Help");
        MenuItem howTo = new MenuItem("How to use");
        howTo.setOnAction(e -> showHowToDialog());
        MenuItem constraints = new MenuItem("Constraints");
        constraints.setOnAction(e -> showConstraintsDialog());
        MenuItem about = new MenuItem("About");
        about.setOnAction(e -> showAboutDialog());
        helpMenu.getItems().addAll(howTo, constraints, new SeparatorMenuItem(), about);

        return new MenuBar(fileMenu, exportMenu, actionsMenu, helpMenu);
    }

    private void handleReRunScheduling() {
        if (repo.getCourses().isEmpty() || repo.getClassrooms().isEmpty() || repo.getSlots().isEmpty()) {
            showError("Missing Data",
                    "Please import Students/Courses/Classrooms/Registrations and Slot Configuration before scheduling.");
            return;
        }

        try {
            scheduleController.generateSchedule();
            this.schedule = scheduleController.getSchedule();

            if (this.schedule != null) {
                updateAllViews();
                showInfo("Scheduled", "Exam schedule generated/regenerated successfully.");
                statusLabel.setText("Schedule generated successfully.");
            } else {
                showError("Failed", "Could not generate a valid schedule with current constraints.");
                statusLabel.setText("Schedule generation failed.");
            }
        } catch (RuntimeException ex) {
            this.schedule = null;
            updateAllViews();
            showError("Scheduling Error", "No feasible schedule could be generated\n" + ex.getMessage());
        }
    }

    private void handleImportAll() {
        showImportDialog();
    }

    private void showImportDialog() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Import CSV Files");
        dialog.setHeaderText("Select CSV files to import data.");

        ButtonType importButtonType = new ButtonType("Import", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(importButtonType, ButtonType.CANCEL);

        javafx.scene.layout.GridPane grid = new javafx.scene.layout.GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField studentsField = new TextField();
        studentsField.setPromptText("Students CSV");
        if (studentsPath != null)
            studentsField.setText(studentsPath.toAbsolutePath().toString());

        TextField coursesField = new TextField();
        coursesField.setPromptText("Courses CSV");
        if (coursesPath != null)
            coursesField.setText(coursesPath.toAbsolutePath().toString());

        TextField classroomsField = new TextField();
        classroomsField.setPromptText("Classrooms CSV");
        if (classroomsPath != null)
            classroomsField.setText(classroomsPath.toAbsolutePath().toString());

        TextField registrationsField = new TextField();
        registrationsField.setPromptText("Registrations CSV");
        if (registrationsPath != null)
            registrationsField.setText(registrationsPath.toAbsolutePath().toString());

        TextField slotsField = new TextField();
        slotsField.setPromptText("Slots CSV");
        if (slotConfigPath != null)
            slotsField.setText(slotConfigPath.toAbsolutePath().toString());

        grid.add(new Label("Students:"), 0, 0);
        grid.add(studentsField, 1, 0);
        grid.add(createBrowseButton("Select Students CSV", studentsField), 2, 0);

        grid.add(new Label("Courses:"), 0, 1);
        grid.add(coursesField, 1, 1);
        grid.add(createBrowseButton("Select Courses CSV", coursesField), 2, 1);

        grid.add(new Label("Classrooms:"), 0, 2);
        grid.add(classroomsField, 1, 2);
        grid.add(createBrowseButton("Select Classrooms CSV", classroomsField), 2, 2);

        grid.add(new Label("Registrations:"), 0, 3);
        grid.add(registrationsField, 1, 3);
        grid.add(createBrowseButton("Select Registrations CSV", registrationsField), 2, 3);

        grid.add(new Label("Slots Config:"), 0, 4);
        grid.add(slotsField, 1, 4);
        grid.add(createBrowseButton("Select Slot Configuration CSV", slotsField), 2, 4);

        dialog.getDialogPane().setContent(grid);

        // Enable/Disable import button validation
        javafx.scene.Node loginButton = dialog.getDialogPane().lookupButton(importButtonType);
        // simple validation: ensure fields are not empty
        loginButton.setDisable(true);

        javafx.beans.value.ChangeListener<String> validationListener = (observable, oldValue, newValue) -> {
            boolean invalid = studentsField.getText().trim().isEmpty() ||
                    coursesField.getText().trim().isEmpty() ||
                    classroomsField.getText().trim().isEmpty() ||
                    registrationsField.getText().trim().isEmpty() ||
                    slotsField.getText().trim().isEmpty();
            loginButton.setDisable(invalid);
        };

        studentsField.textProperty().addListener(validationListener);
        coursesField.textProperty().addListener(validationListener);
        classroomsField.textProperty().addListener(validationListener);
        registrationsField.textProperty().addListener(validationListener);
        slotsField.textProperty().addListener(validationListener);

        // Trigger initial check
        validationListener.changed(null, null, null);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == importButtonType) {
                try {
                    this.studentsPath = Paths.get(studentsField.getText().trim());
                    this.coursesPath = Paths.get(coursesField.getText().trim());
                    this.classroomsPath = Paths.get(classroomsField.getText().trim());
                    this.registrationsPath = Paths.get(registrationsField.getText().trim());
                    this.slotConfigPath = Paths.get(slotsField.getText().trim());
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            return null;
        });

        Optional<Boolean> result = dialog.showAndWait();

        if (result.isPresent() && result.get()) {
            try {
                importService.importAll(studentsPath, coursesPath, classroomsPath, registrationsPath, slotConfigPath);
                refreshAllDataControllers();

                // Try scheduling immediately.
                scheduleController.generateSchedule();
                this.schedule = scheduleController.getSchedule();

                updateAllViews();
                if (this.schedule != null) {
                    showInfo("Import & Scheduling", "CSV files imported and schedule generated successfully.");
                } else {
                    showInfo(
                            "Import Completed",
                            "CSV files have been imported successfully.\n\n" +
                                    "If no schedule is visible, please use:\n" +
                                    "Actions → Run / Re-run Scheduling");
                }

            } catch (Exception ex) {
                this.schedule = null;
                updateAllViews();
                showError("Import/Scheduling Error", "Operation failed\n" + ex.getMessage());
            }
        }
    }

    private Button createBrowseButton(String title, TextField targetField) {
        Button btn = new Button("Browse...");
        btn.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle(title);
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            String currentText = targetField.getText();
            if (!currentText.isEmpty()) {
                try {
                    File f = new File(currentText);
                    if (f.exists()) {
                        chooser.setInitialDirectory(f.getParentFile());
                    }
                } catch (Exception ignored) {
                }
            }

            File file = chooser.showOpenDialog(primaryStage);
            if (file != null) {
                targetField.setText(file.getAbsolutePath());
            }
        });
        return btn;
    }

    private void handleImportSlotsOnly() {
        Path slots = chooseCsvFile("Select Slot Configuration CSV", slotConfigPath);
        if (slots == null)
            return;

        try {
            this.slotConfigPath = slots;
            repo.loadSlots(slots);

            // Slot config changed -> try to re-schedule if we have enough data.
            // 5. Generate Initial Schedule
            scheduleController.generateSchedule();
            this.schedule = scheduleController.getSchedule();

            if (this.schedule != null) {
                showInfo("Success", "Data loaded and schedule generated successfully.");
            } else {
                showInfo("Loaded", "Data loaded. Constraints may prevent full scheduling.");
            }
            updateAllViews();
            showInfo("Slot Configuration", "Slot configuration imported successfully.");
        } catch (Exception ex) {
            this.schedule = null;
            updateAllViews();
            showError("Slot Configuration Error", "Failed to import slot configuration\n" + ex.getMessage());
        }
    }

    private Path chooseCsvFile(String title, Path lastPath) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(title);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        if (lastPath != null) {
            try {
                File parent = lastPath.toFile().getParentFile();
                if (parent != null && parent.exists()) {
                    chooser.setInitialDirectory(parent);
                }
            } catch (Exception ignored) {
            }
        }

        File file = chooser.showOpenDialog(primaryStage);
        return file == null ? null : file.toPath();
    }

    private void showHowToDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("How to use");
        alert.setHeaderText("Exam Scheduler - User Guide");

        String msg = "1) File > Import CSV Files...\n" +
                "   - Students, Courses, Classrooms, Registrations, Slot Config\n\n" +
                "2) Actions > Run / Re-run Scheduling\n" +
                "   - Generates a schedule respecting mandatory constraints\n\n" +
                "3) Slot Configuration tab\n" +
                "   - Edit day count & time ranges, save and re-run\n\n" +
                "4) By Course tab\n" +
                "   - You may edit Day/Slot; the system blocks constraint violations\n\n" +
                "5) Export menu\n" +
                "   - Export schedule views as CSV";

        TextArea ta = new TextArea(msg);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefWidth(650);
        ta.setPrefHeight(380);
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void showConstraintsDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Constraints");
        alert.setHeaderText("Mandatory constraints");

        String msg = "Hard constraints (never relaxed):\n" +
                "- A student cannot have 2 consecutive slots on the same day.\n" +
                "- A student cannot have more than 2 exams in a day.\n" +
                "- Same slot: common students cannot overlap.\n" +
                "- Same slot: a classroom cannot be assigned to 2 exams.\n\n" +
                "If no feasible schedule exists, the application reports 'No feasible schedule'.";

        TextArea ta = new TextArea(msg);
        ta.setEditable(false);
        ta.setWrapText(true);
        ta.setPrefWidth(650);
        ta.setPrefHeight(280);
        alert.getDialogPane().setContent(ta);
        alert.showAndWait();
    }

    private void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("About");
        alert.setHeaderText("Desktop Exam Scheduling Assistant");
        alert.setContentText(
                "SE302 Project - Student Affairs Scheduling Tool\n" +
                        "Imports CSV data, generates a feasible exam schedule, and exports views.");
        alert.showAndWait();
    }

    private void updateAllViews() {
        BorderPane root = (BorderPane) primaryStage.getScene().getRoot(); // Use primaryStage to get the scene
        root.setTop(createMenuBar());
        TabPane tabPane = (TabPane) root.getCenter();

        int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();

        tabPane.getTabs().clear();
        tabPane.getTabs().add(createDataManagementTab());
        tabPane.getTabs().add(createSlotConfigurationTab());
        tabPane.getTabs().add(new ScheduleByCourseView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByRoomView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByStudentView(scheduleController).createTab());
        tabPane.getTabs().add(new ScheduleByDaySlotView(scheduleController).createTab());
        tabPane.getTabs().add(new StudentScheduleView(scheduleController, repo).createTab());

        tabPane.getSelectionModel().select(selectedIndex);
    }

    private void handleExportByCourse() {
        try {
            exportService.exportByCourse(
                    schedule,
                    Paths.get("schedule_by_course.csv"));
            showInfo("Export", "Schedule (By Course) has been saved as a CSV file.");
        } catch (IOException e) {
            showError("Export Error", "An error occurred during By Course export:\n" + e.getMessage());
        }
    }

    private void handleExportByRoom() {
        try {
            exportService.exportByRoom(
                    schedule,
                    Paths.get("schedule_by_room.csv"));
            showInfo("Export", "Schedule (By Room) has been saved as a CSV file.");
        } catch (IOException e) {
            showError("Export Error", "An error occurred during By Room export:\n" + e.getMessage());
        }
    }

    private void handleExportByStudent() {
        try {
            exportService.exportByStudent(
                    schedule,
                    Paths.get("schedule_by_student.csv"));
            showInfo("Export", "Schedule (By Student) has been saved as a CSV file.");
        } catch (IOException e) {
            showError("Export Error", "An error occurred during By Student export:\n" + e.getMessage());
        }
    }

    private void handleExportByDaySlot() {
        try {
            exportService.exportByDaySlot(
                    schedule,
                    Paths.get("schedule_by_day_slot.csv"));
            showInfo("Export", "Schedule (By Day/Slot) has been saved as a CSV file.");
        } catch (IOException e) {
            showError("Export Error", "An error occurred during By Day/Slot export:\n" + e.getMessage());
        }
    }

    private Tab createStudentManagementTab() {
        if (studentController == null) {
            studentController = new StudentController(repo);
        }
        return new StudentTabView(studentController).createTab();
    }

    private Tab createCourseManagementTab() {
        if (courseController == null) {
            courseController = new CourseController(repo);
        }
        return new CourseTabView(courseController).createTab();
    }

    private Tab createRegistrationManagementTab() {
        if (registrationController == null) {
            registrationController = new RegistrationController(repo);
        }
        return new RegistrationTabView(registrationController).createTab();
    }

    private Tab createClassroomManagementTab() {
        if (classroomController == null) {
            classroomController = new ClassroomController(repo);
        }
        return new ClassroomTabView(classroomController).createTab();
    }

    private Tab createSlotConfigurationTab() {
        if (slotController == null) {
            slotController = new SlotController(repo);
        }
        return new SlotTabView(slotController, primaryStage, newSchedule -> {
            this.schedule = newSchedule;
            scheduleController.setSchedule(newSchedule); // Sync controller
            updateAllViews();
        }).createTab();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        if (statusLabel != null)
            statusLabel.setText(message.replace("\n", " "));
    }


    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
        if (statusLabel != null)
            statusLabel.setText(title + ": " + message.replace("\n", " "));
    }

    public static void main(String[] args) {
        launch(args);
    }
}