import javafx.application.Application;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Path;
import javafx.scene.control.Alert.AlertType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Spinner;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.converter.IntegerStringConverter;
import javafx.geometry.Insets;
import javafx.scene.control.TableCell;

public class ExamSchedulerApp extends Application {

    private Stage primaryStage;

    private DataRepository repo;
    private CsvImportService importService;
    private CsvExportService exportService;

    // Last imported files (optional, for re-import/re-run)
    private Path studentsPath;
    private Path coursesPath;
    private Path classroomsPath;
    private Path registrationsPath;
    private Path slotConfigPath;

    private ObservableList<SlotConfigurationRow> slotData;
    private Spinner<Integer> dayCountSpinner;
    private Schedule schedule;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        repo = new DataRepository();
        this.importService = new CsvImportService(repo);
        this.exportService = new CsvExportService(repo);

        // Try to load sample data from the working directory (optional).
        tryAutoLoadDefaultSampleData();

        // If we already have data, try to schedule once.
        if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
            try {
                this.schedule = new SchedulingEngine(repo).generateExamSchedule();
            } catch (RuntimeException ex) {
                this.schedule = null;
                showError("Scheduling Error",
                        "No feasible schedule could be generated with current data\n" + ex.getMessage());
            }
        }

        TabPane tabPane = new TabPane();
        tabPane.getTabs().add(createDataManagementTab());
        tabPane.getTabs().add(createSlotConfigurationTab());
        tabPane.getTabs().add(createByCourseTab(this.schedule));
        tabPane.getTabs().add(createByRoomTab(this.schedule));
        tabPane.getTabs().add(createByStudentTab(this.schedule));
        tabPane.getTabs().add(createByDaySlotTab(this.schedule));
        tabPane.getTabs().add(createStudentScheduleTab());


        BorderPane root = new BorderPane();
        root.setTop(createMenuBar());
        root.setCenter(tabPane);

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
            }
        } catch (IOException e) {
            showError("CSV Import Error",
                    "An error occurred while importing default CSV files\n" + e.getMessage());
        }
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
            if (primaryStage != null) primaryStage.close();
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
            SchedulingEngine engine = new SchedulingEngine(this.repo);
            this.schedule = engine.generateExamSchedule();
            updateAllViews();
            showInfo("Success", "Schedule generated successfully.");
        } catch (RuntimeException ex) {
            this.schedule = null;
            updateAllViews();
            showError("Scheduling Error", "No feasible schedule could be generated\n" + ex.getMessage());
        }
    }

    private void handleImportAll() {
        Path s = chooseCsvFile("Select Students CSV", studentsPath);
        if (s == null) return;
        Path c = chooseCsvFile("Select Courses CSV", coursesPath);
        if (c == null) return;
        Path r = chooseCsvFile("Select Classrooms CSV", classroomsPath);
        if (r == null) return;
        Path a = chooseCsvFile("Select Registrations / Attendance CSV", registrationsPath);
        if (a == null) return;
        Path slots = chooseCsvFile("Select Slot Configuration CSV", slotConfigPath);
        if (slots == null) return;

        try {
            this.studentsPath = s;
            this.coursesPath = c;
            this.classroomsPath = r;
            this.registrationsPath = a;
            this.slotConfigPath = slots;

            importService.importAll(s, c, r, a, slots);

            // Try scheduling immediately.
            this.schedule = new SchedulingEngine(repo).generateExamSchedule();

            updateAllViews();
            showInfo("Import & Scheduling", "CSV files imported and schedule generated successfully.");
        } catch (Exception ex) {
            this.schedule = null;
            updateAllViews();
            showError("Import/Scheduling Error", "Operation failed\n" + ex.getMessage());
        }
    }

    private void handleImportSlotsOnly() {
        Path slots = chooseCsvFile("Select Slot Configuration CSV", slotConfigPath);
        if (slots == null) return;

        try {
            this.slotConfigPath = slots;
            repo.loadSlots(slots);

            // Slot config changed -> try to re-schedule if we have enough data.
            if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
                this.schedule = new SchedulingEngine(repo).generateExamSchedule();
            } else {
                this.schedule = null;
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
            } catch (Exception ignored) { }
        }

        File file = chooser.showOpenDialog(primaryStage);
        return file == null ? null : file.toPath();
    }
    private void showHowToDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("How to use");
        alert.setHeaderText("Exam Scheduler - User Guide");

        String msg =
                "1) File > Import CSV Files...\n" +
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

        String msg =
                "Hard constraints (never relaxed):\n" +
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
        BorderPane root = (BorderPane) dayCountSpinner.getScene().getRoot();
        root.setTop(createMenuBar());
        TabPane tabPane = (TabPane) root.getCenter();

        int selectedIndex = tabPane.getSelectionModel().getSelectedIndex();

        tabPane.getTabs().clear();
        tabPane.getTabs().add(createDataManagementTab());
        tabPane.getTabs().add(createSlotConfigurationTab());
        tabPane.getTabs().add(createByCourseTab(this.schedule));
        tabPane.getTabs().add(createByRoomTab(this.schedule));
        tabPane.getTabs().add(createByStudentTab(this.schedule));
        tabPane.getTabs().add(createByDaySlotTab(this.schedule));
        tabPane.getTabs().add(createStudentScheduleTab());


        tabPane.getSelectionModel().select(selectedIndex);
    }

    private void handleExportByCourse() {
        try {
            exportService.exportByCourse(
                    schedule,
                    Paths.get("schedule_by_course.csv")
            );
            showInfo("Export", "Schedule (By Course) CSV olarak kaydedildi.");
        } catch (IOException e) {
            showError("Export Error", "By Course export sırasında hata:\n" + e.getMessage());
        }
    }

    private void handleExportByRoom() {
        try {
            exportService.exportByRoom(
                    schedule,
                    Paths.get("schedule_by_room.csv")
            );
            showInfo("Export", "Schedule (By Room) CSV olarak kaydedildi.");
        } catch (IOException e) {
            showError("Export Error", "By Room export sırasında hata:\n" + e.getMessage());
        }
    }

    private void handleExportByStudent() {
        try {
            exportService.exportByStudent(
                    schedule,
                    Paths.get("schedule_by_student.csv")
            );
            showInfo("Export", "Schedule (By Student) CSV olarak kaydedildi.");
        } catch (IOException e) {
            showError("Export Error", "By Student export sırasında hata:\n" + e.getMessage());
        }
    }

    private void handleExportByDaySlot() {
        try {
            exportService.exportByDaySlot(
                    schedule,
                    Paths.get("schedule_by_day_slot.csv")
            );
            showInfo("Export", "Schedule (By Day/Slot) CSV olarak kaydedildi.");
        } catch (IOException e) {
            showError("Export Error", "By Day/Slot export sırasında hata:\n" + e.getMessage());
        }
    }
    private Tab createStudentManagementTab() {
        BorderPane root = new BorderPane();

        TableView<StudentRow> table = new TableView<>();
        ObservableList<StudentRow> data = FXCollections.observableArrayList();

        for (Student s : repo.getStudents().values()) {
            data.add(new StudentRow(s.getStudentId()));
        }
        data.sort(Comparator.comparing(StudentRow::getStudentId));
        table.setItems(data);

        TableColumn<StudentRow, String> idCol = new TableColumn<>("Student ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        table.getColumns().add(idCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField newStudentField = new TextField();
        newStudentField.setPromptText("Yeni öğrenci ID'si (ör. Std_ID_999)");

        Button addButton = new Button("➕ Ekle");
        addButton.setOnAction(e -> {
            String id = newStudentField.getText().trim();
            if (id.isEmpty()) {
                showError("Geçersiz ID", "Öğrenci ID'si boş olamaz.");
                return;
            }
            boolean ok = repo.addStudent(id);
            if (!ok) {
                showError("Ekleme Başarısız", "Bu ID'ye sahip bir öğrenci zaten var.");
                return;
            }
            data.add(new StudentRow(id));
            data.sort(Comparator.comparing(StudentRow::getStudentId));
            newStudentField.clear();
        });

        Button removeButton = new Button("🗑️ Seçileni Sil");
        removeButton.setOnAction(e -> {
            StudentRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Seçim Yok", "Lütfen silmek için bir öğrenci seçin.");
                return;
            }
            String id = selected.getStudentId();
            boolean ok = repo.removeStudent(id);
            if (!ok) {
                showError("Silme Başarısız", "Öğrenci silinemedi.");
                return;
            }
            data.remove(selected);
        });

        Label info = new Label(
                "Not: Öğrenciler bellekte güncellenir; yeni sınav programı için schedule'ı tekrar üretmeniz gerekir."
        );
        info.setWrapText(true);

        HBox buttons = new HBox(10,
                new Label("Yeni öğrenci:"), newStudentField, addButton, removeButton);
        buttons.setPadding(new Insets(10));

        VBox bottom = new VBox(5, buttons, info);

        root.setCenter(table);
        root.setBottom(bottom);

        Tab tab = new Tab("Students", root);
        tab.setClosable(false);
        return tab;
    }
    private Tab createCourseManagementTab() {
        BorderPane root = new BorderPane();

        TableView<CourseRow> table = new TableView<>();
        ObservableList<CourseRow> data = FXCollections.observableArrayList();

        for (Course c : repo.getCourses().values()) {
            data.add(new CourseRow(c.getCourseCode(), c.getStudentCount()));
        }
        data.sort(Comparator.comparing(CourseRow::getCourseCode));
        table.setItems(data);

        TableColumn<CourseRow, String> codeCol = new TableColumn<>("Course Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<CourseRow, Integer> countCol = new TableColumn<>("Student Count");
        countCol.setCellValueFactory(new PropertyValueFactory<>("studentCount"));

        table.getColumns().addAll(codeCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TextField newCourseField = new TextField();
        newCourseField.setPromptText("Yeni ders kodu (ör. CourseCode_99)");

        Button addButton = new Button("➕ Ders Ekle");
        addButton.setOnAction(e -> {
            String code = newCourseField.getText().trim();
            if (code.isEmpty()) {
                showError("Geçersiz Kod", "Ders kodu boş olamaz.");
                return;
            }
            boolean ok = repo.addCourse(code);
            if (!ok) {
                showError("Ekleme Başarısız", "Bu kodla zaten bir ders var.");
                return;
            }
            data.add(new CourseRow(code, 0));
            data.sort(Comparator.comparing(CourseRow::getCourseCode));
            newCourseField.clear();
        });

        Button removeButton = new Button("🗑️ Seçileni Sil");
        removeButton.setOnAction(e -> {
            CourseRow selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                showError("Seçim Yok", "Lütfen silmek için bir ders seçin.");
                return;
            }
            String code = selected.getCourseCode();
            boolean ok = repo.removeCourse(code);
            if (!ok) {
                showError("Silme Başarısız", "Ders silinemedi.");
                return;
            }
            data.remove(selected);
        });

        Label info = new Label(
                "Not: Ders listesi bellekte güncellenir; yeni program için schedule'ı yeniden üretmeniz gerekir."
        );
        info.setWrapText(true);

        HBox buttons = new HBox(10,
                new Label("Yeni ders:"), newCourseField, addButton, removeButton);
        buttons.setPadding(new Insets(10));

        VBox bottom = new VBox(5, buttons, info);

        root.setCenter(table);
        root.setBottom(bottom);

        Tab tab = new Tab("Courses", root);
        tab.setClosable(false);
        return tab;
    }

    private Tab createStudentScheduleTab() {

        BorderPane root = new BorderPane();

        // -------------------------------
        // ÜST KISIM: ÖĞRENCİ SEÇME
        // -------------------------------
        ComboBox<String> studentBox = new ComboBox<>();
        studentBox.setPromptText("Select student");
        List<String> studentIds = new ArrayList<>(repo.getStudents().keySet());

        studentIds.sort(Comparator.comparingInt(id -> {
            String numberPart = id.replaceAll("\\D+", "");
            return Integer.parseInt(numberPart);
        }));

        studentBox.getItems().addAll(studentIds);

        VBox top = new VBox(10);
        top.setPadding(new Insets(10));
        top.getChildren().addAll(
                new Label("Select a student:"),
                studentBox
        );
        root.setTop(top);

        // -------------------------------
        // ORTA KISIM: TABLO
        // -------------------------------
        TableView<StudentScheduleRow> table = new TableView<>();
        ObservableList<StudentScheduleRow> data = FXCollections.observableArrayList();
        table.setItems(data);

        TableColumn<StudentScheduleRow, String> c1 = new TableColumn<>("Course");
        c1.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

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

        // -------------------------------
        // 🔴 ADIM 9 — ASIL OLAY BURASI
        // Öğrenci seçilince tabloyu doldur
        // -------------------------------
        studentBox.setOnAction(e -> {

            data.clear(); // önce tabloyu temizle

            String studentId = studentBox.getValue();
            if (studentId == null) return;

            for (Exam exam : schedule.getAllExams()) {

                // Bu öğrenci bu derse kayıtlı mı?
                if (exam.getCourse().getStudentIds().contains(studentId)) {

                    // Room ID'leri birleştir
                    String rooms = exam.getAssignedRooms()
                            .stream()
                            .map(Classroom::getRoomId)
                            .collect(Collectors.joining(","));

                    // Tabloya satır ekle
                    data.add(new StudentScheduleRow(
                            studentId,                               // 1️⃣ studentId
                            exam.getCourse().getCourseCode(),        // 2️⃣ courseCode
                            exam.getSlot().getDay(),                 // 3️⃣ day
                            exam.getSlot().getIndex(),               // 4️⃣ slot
                            exam.getSlot().getTimeRange(),           // 5️⃣ time
                            rooms                                    // 6️⃣ rooms
                    ));

                }
            }
        });

        // -------------------------------
        // TAB
        // -------------------------------
        Tab tab = new Tab("Student Schedule");
        tab.setClosable(false);
        tab.setContent(root);

        return tab;
    }



    private Tab createRegistrationManagementTab() {
        BorderPane root = new BorderPane();

        ComboBox<String> studentCombo = new ComboBox<>();
        ComboBox<String> courseCombo = new ComboBox<>();

        List<String> studentIds = new ArrayList<>(repo.getStudents().keySet());
        Collections.sort(studentIds);
        studentCombo.setItems(FXCollections.observableArrayList(studentIds));

        List<String> courseCodes = new ArrayList<>(repo.getCourses().keySet());
        Collections.sort(courseCodes);
        courseCombo.setItems(FXCollections.observableArrayList(courseCodes));

        studentCombo.setPromptText("Öğrenci seç");
        courseCombo.setPromptText("Ders seç");

        Button registerBtn = new Button("📌 Register");
        Button unregisterBtn = new Button("❌ Unregister");
        Button refreshBtn = new Button("🔄 Refresh");

        // Tablo: hangi öğrenci hangi derste
        TableView<RegistrationRow> table = new TableView<>();
        ObservableList<RegistrationRow> data = FXCollections.observableArrayList();
        rebuildRegistrationData(data);

        table.setItems(data);

        TableColumn<RegistrationRow, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

        TableColumn<RegistrationRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        table.getColumns().addAll(studentCol, courseCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        registerBtn.setOnAction(e -> {
            String sid = studentCombo.getValue();
            String code = courseCombo.getValue();
            if (sid == null || code == null) {
                showError("Eksik Seçim", "Hem öğrenci hem ders seçmelisiniz.");
                return;
            }

            boolean ok = repo.registerStudentToCourse(sid, code);
            if (!ok) {
                showError("Kayıt Başarısız",
                        "Öğrenci bulunamadı veya zaten bu derse kayıtlı.");
                return;
            }
            data.add(new RegistrationRow(sid, code));
        });

        unregisterBtn.setOnAction(e -> {
            String sid = studentCombo.getValue();
            String code = courseCombo.getValue();
            if (sid == null || code == null) {
                showError("Eksik Seçim", "Hem öğrenci hem ders seçmelisiniz.");
                return;
            }

            boolean ok = repo.unregisterStudentFromCourse(sid, code);
            if (!ok) {
                showError("Silme Başarısız",
                        "Bu öğrenci bu derse kayıtlı görünmüyor.");
                return;
            }

            RegistrationRow toRemove = null;
            for (RegistrationRow r : data) {
                if (r.getStudentId().equals(sid) && r.getCourseCode().equals(code)) {
                    toRemove = r;
                    break;
                }
            }
            if (toRemove != null) data.remove(toRemove);
        });

        refreshBtn.setOnAction(e -> {
            List<String> newStudentIds = new ArrayList<>(repo.getStudents().keySet());
            Collections.sort(newStudentIds);
            studentCombo.setItems(FXCollections.observableArrayList(newStudentIds));

            List<String> newCourseCodes = new ArrayList<>(repo.getCourses().keySet());
            Collections.sort(newCourseCodes);
            courseCombo.setItems(FXCollections.observableArrayList(newCourseCodes));

            rebuildRegistrationData(data);
        });

        HBox controls = new HBox(10,
                new Label("Öğrenci:"), studentCombo,
                new Label("Ders:"), courseCombo,
                registerBtn, unregisterBtn, refreshBtn);
        controls.setPadding(new Insets(10));

        Label info = new Label(
                "Not: Kayıt değişiklikleri Course nesnelerindeki öğrenci listesine yansır ve yeni schedule üretiminde kullanılabilir."
        );
        info.setWrapText(true);

        VBox center = new VBox(10, table, info);
        center.setPadding(new Insets(10));

        root.setTop(controls);
        root.setCenter(center);

        Tab tab = new Tab("Registrations", root);
        tab.setClosable(false);
        return tab;
    }

    // repo’daki course -> student listesine göre tabloyu yeniden doldur
    private void rebuildRegistrationData(ObservableList<RegistrationRow> data) {
        data.clear();
        for (Course c : repo.getCourses().values()) {
            for (String sid : c.getStudentIds()) {
                data.add(new RegistrationRow(sid, c.getCourseCode()));
            }
        }
        data.sort(Comparator
                .comparing(RegistrationRow::getCourseCode)
                .thenComparing(RegistrationRow::getStudentId));
    }
    private Tab createClassroomManagementTab() {
        BorderPane root = new BorderPane();

        TableView<ClassroomRow> table = new TableView<>();
        ObservableList<ClassroomRow> data = FXCollections.observableArrayList();

        for (Classroom room : repo.getClassrooms()) {
            data.add(new ClassroomRow(room.getRoomId(), room.getCapacity()));
        }
        data.sort(Comparator.comparing(ClassroomRow::getRoomId));
        table.setItems(data);

        TableColumn<ClassroomRow, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<ClassroomRow, Integer> capCol = new TableColumn<>("Capacity");
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        capCol.setOnEditCommit(evt -> {
            ClassroomRow row = evt.getRowValue();
            Integer newCap = evt.getNewValue();
            if (newCap == null || newCap <= 0) {
                showError("Geçersiz Kapasite", "Kapasite pozitif bir sayı olmalıdır.");
                table.refresh();
                return;
            }
            boolean ok = repo.updateClassroomCapacity(row.getRoomId(), newCap);
            if (!ok) {
                showError("Güncelleme Başarısız", "Kapasite güncellenemedi.");
                table.refresh();
                return;
            }
            row.setCapacity(newCap);
        });

        table.getColumns().addAll(roomCol, capCol);
        table.setEditable(true);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Label info = new Label(
                "Not: Sınıf kapasitesi değişiklikleri bellekte tutulur; gerekirse yeni schedule alın."
        );
        info.setWrapText(true);

        VBox vbox = new VBox(10, table, info);
        vbox.setPadding(new Insets(10));

        Tab tab = new Tab("Classrooms", vbox);
        tab.setClosable(false);
        return tab;
    }




    private Tab createSlotConfigurationTab() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));

        Label title = new Label("📅 Slot ve Gün Yapılandırması ");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox controls = createSlotControls();

        TableView<SlotConfigurationRow> slotTable = createSlotTable();
        loadInitialSlotData();   // <-- Artık tabloyu repo’dan dolduruyoruz

        mainLayout.getChildren().addAll(title, controls, slotTable);

        Tab tab = new Tab("Slot Config ", mainLayout);
        tab.setClosable(false);
        return tab;
    }


    private HBox createSlotControls() {
        HBox controls = new HBox(15);


        Label dayLabel = new Label("Toplam Sınav Günü:");
        this.dayCountSpinner = new Spinner<>(1, 10, 5);
        dayCountSpinner.setPrefWidth(70);


        Button addNewSlotButton = new Button("➕ Yeni Slot Ekle");
        addNewSlotButton.setOnAction(e -> addNewSlotRow());


        Button saveButton = new Button("💾 Konfigürasyonu Kaydet");
        saveButton.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white;");
        saveButton.setOnAction(e -> handleSaveConfiguration());

        controls.getChildren().addAll(dayLabel, dayCountSpinner, addNewSlotButton, saveButton);
        return controls;
    }

    private TableView<SlotConfigurationRow> createSlotTable() {
        TableView<SlotConfigurationRow> table = new TableView<>();
        table.setEditable(true);

        this.slotData = FXCollections.observableArrayList();
        table.setItems(this.slotData);

        // Day sütunu: sadece gösterim amaçlı, hep 1 olacak
        TableColumn<SlotConfigurationRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<SlotConfigurationRow, Integer> slotIndexCol = new TableColumn<>("Slot Index");
        slotIndexCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<SlotConfigurationRow, String> startTimeCol = new TableColumn<>("Start Time (HH:MM)");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        startTimeCol.setOnEditCommit(event -> {
            SlotConfigurationRow row = event.getRowValue();
            row.setStartTime(event.getNewValue());
        });

        TableColumn<SlotConfigurationRow, String> endTimeCol = new TableColumn<>("End Time (HH:MM)");
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        endTimeCol.setOnEditCommit(event -> {
            SlotConfigurationRow row = event.getRowValue();
            row.setEndTime(event.getNewValue());
        });

        // Sil butonu
        TableColumn<SlotConfigurationRow, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<SlotConfigurationRow, Void>() {
            private final Button deleteButton = new Button("🗑️ Sil");
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

        table.getColumns().addAll(dayCol, slotIndexCol, startTimeCol, endTimeCol, actionCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        return table;
    }


    private void loadInitialSlotData() {
        slotData.clear();

        List<Slot> slots = repo.getSlots();

        if (slots == null || slots.isEmpty()) {
            // Hiç slot yoksa basit bir default ver:
            dayCountSpinner.getValueFactory().setValue(5);
            slotData.add(new SlotConfigurationRow(1, 1, "09:00", "11:00"));
            slotData.add(new SlotConfigurationRow(1, 2, "14:00", "16:00"));
            slotData.add(new SlotConfigurationRow(1, 3, "19:00", "21:00"));
            return;
        }

        // Gün sayısını hesapla
        int maxDay = slots.stream().mapToInt(Slot::getDay).max().orElse(1);
        dayCountSpinner.getValueFactory().setValue(maxDay);

        // Her slot index için bir time range al (tüm günlerde aynı olduğunu varsayıyoruz)
        Map<Integer, String> indexToRange = new TreeMap<>();
        for (Slot s : slots) {
            indexToRange.putIfAbsent(s.getIndex(), s.getTimeRange());
        }

        for (Map.Entry<Integer, String> entry : indexToRange.entrySet()) {
            int slotIndex = entry.getKey();
            String range = entry.getValue();

            String[] parts = range.split("-");
            String start = parts.length > 0 ? parts[0].trim() : "";
            String end   = parts.length > 1 ? parts[1].trim() : "";

            slotData.add(new SlotConfigurationRow(1, slotIndex, start, end));
        }
    }

    private void addNewSlotRow() {
        int nextSlotIndex = slotData.stream()
                .mapToInt(SlotConfigurationRow::getSlotIndex)
                .max().orElse(0) + 1;

        slotData.add(new SlotConfigurationRow(1, nextSlotIndex, "00:00", "00:00"));
    }


    private void handleSaveConfiguration() {
        int numDays = dayCountSpinner.getValue();

        if (slotData.isEmpty()) {
            showError("Geçersiz Konfigürasyon",
                    "En az bir slot tanımlamanız gerekiyor.");
            return;
        }

        // Slotları index’e göre sırala
        List<SlotConfigurationRow> rows = new ArrayList<>(slotData);
        rows.sort(Comparator.comparingInt(SlotConfigurationRow::getSlotIndex));

        List<String> timeRanges = new ArrayList<>();

        for (SlotConfigurationRow row : rows) {
            String startTime = row.getStartTime().trim();
            String endTime = row.getEndTime().trim();

            if (startTime.isEmpty() || endTime.isEmpty()) {
                showError("Eksik Zaman Bilgisi",
                        "Tüm slotlar için başlangıç ve bitiş saatleri doldurulmalıdır.");
                return;
            }

            String range = startTime + "-" + endTime;
            timeRanges.add(range);
        }

        // 1) Repo içindeki slot listesini güncelle
        List<Slot> newSlots = SlotGenerator.generateSlots(numDays, timeRanges);
        repo.setSlots(newSlots);

        // 2) CSV dosyasını FR1 formatında yeniden yaz (seçili dosyaya)
        java.nio.file.Path out = this.slotConfigPath;
        if (out == null) {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Slot Configuration CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            File file = chooser.showSaveDialog(primaryStage);
            if (file == null) {
                return;
            }
            out = file.toPath();
            this.slotConfigPath = out;
        }

        try (BufferedWriter bw = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            bw.write(Integer.toString(numDays));
            for (String tr : timeRanges) {
                bw.write(";");
                bw.write(tr);
            }
            bw.newLine();
        } catch (Exception e) {
            showError("Dosya Yazma Hatası",
                    "slot konfigürasyonu dosyaya kaydedilemedi\n" + e.getMessage());
            return;
        }

        // Slotlar değişti -> mümkünse otomatik yeniden schedule üret
        boolean rescheduled = false;
        try {
            if (!repo.getCourses().isEmpty() && !repo.getClassrooms().isEmpty() && !repo.getSlots().isEmpty()) {
                this.schedule = new SchedulingEngine(repo).generateExamSchedule();
                rescheduled = true;
            } else {
                this.schedule = null;
            }
        } catch (RuntimeException ex) {
            this.schedule = null;
        }

        updateAllViews();

        showInfo("Kaydedildi",
                "Slot konfigürasyonu başarıyla kaydedildi.\n" +
                        "Gün sayısı: " + numDays +
                        ", Günlük slot sayısı: " + timeRanges.size() +
                        (rescheduled ? "\n\nSchedule otomatik olarak yeniden oluşturuldu." : "\n\nNot: Schedule oluşturmak için Actions > Run / Re-run Scheduling kullanın."));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



    private Tab createByCourseTab(Schedule schedule) {

        if (schedule == null) {
            Tab tab = new Tab("By Course", new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }

        TableView<CourseScheduleRow> table = new TableView<>();
        table.setEditable(true); // tabloyu editleme şekli

        ObservableList<CourseScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            String courseCode = exam.getCourse().getCourseCode();
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String rooms = joinRoomIds(exam.getAssignedRooms());
            int studentCount = exam.getCourse().getStudentCount();

            items.add(new CourseScheduleRow(courseCode, day, slotIndex, timeRange, rooms, studentCount));
        }

        items.sort(Comparator
                .comparingInt(CourseScheduleRow::getDay)
                .thenComparingInt(CourseScheduleRow::getSlotIndex)
                .thenComparing(CourseScheduleRow::getCourseCode));

        table.setItems(items);

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
                // ➜ değişikliği gerçek schedule'a uygula
                applyRowToSchedule(row);
            } else {
                table.refresh(); // geçersizse eski haline dön
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
                // ➜ değişikliği gerçek schedule'a uygula
                applyRowToSchedule(row);
            } else {
                table.refresh(); // geçersizse eski haline dön
            }
        });

        TableColumn<CourseScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<CourseScheduleRow, String> roomCol = new TableColumn<>("Rooms");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("rooms"));

        TableColumn<CourseScheduleRow, Integer> countCol = new TableColumn<>("Students");
        countCol.setCellValueFactory(new PropertyValueFactory<>("studentCount"));

        table.getColumns().addAll(courseCol, dayCol, slotCol, timeCol, roomCol, countCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Tab tab = new Tab("By Course", table);
        tab.setClosable(false);
        return tab;
    }




    private Tab createByRoomTab(Schedule schedule) {

        if (schedule == null) {
            Tab tab = new Tab("By Room", new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }


        TableView<RoomScheduleRow> table = new TableView<>();
        ObservableList<RoomScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String courseCode = exam.getCourse().getCourseCode();

            for (Classroom room : exam.getAssignedRooms()) {
                items.add(new RoomScheduleRow(room.getRoomId(), day, slotIndex, timeRange, courseCode));
            }
        }

        items.sort(Comparator
                .comparing(RoomScheduleRow::getRoomId)
                .thenComparingInt(RoomScheduleRow::getDay)
                .thenComparingInt(RoomScheduleRow::getSlotIndex));

        table.setItems(items);

        TableColumn<RoomScheduleRow, String> roomCol = new TableColumn<>("Room");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("roomId"));

        TableColumn<RoomScheduleRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));

        TableColumn<RoomScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<RoomScheduleRow, String> timeCol = new TableColumn<>("Time");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timeRange"));

        TableColumn<RoomScheduleRow, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        table.getColumns().addAll(roomCol, dayCol, slotCol, timeCol, courseCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Tab tab = new Tab("By Room", table);
        tab.setClosable(false);
        return tab;
    }

    private Tab createByStudentTab(Schedule schedule) {

        if (schedule == null) {
            Tab tab = new Tab("By Student", new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
            tab.setClosable(false);
            return tab;
        }


        TableView<StudentScheduleRow> table = new TableView<>();
        ObservableList<StudentScheduleRow> items = FXCollections.observableArrayList();

        for (Exam exam : schedule.getAllExams()) {
            String courseCode = exam.getCourse().getCourseCode();
            int day = exam.getSlot().getDay();
            int slotIndex = exam.getSlot().getIndex();
            String timeRange = exam.getSlot().getTimeRange();
            String rooms = joinRoomIds(exam.getAssignedRooms());

            for (String studentId : exam.getCourse().getStudentIds()) {
                items.add(new StudentScheduleRow(studentId, courseCode, day, slotIndex, timeRange, rooms));
            }
        }

        items.sort(Comparator
                .comparing(StudentScheduleRow::getStudentId)
                .thenComparingInt(StudentScheduleRow::getDay)
                .thenComparingInt(StudentScheduleRow::getSlotIndex));

        table.setItems(items);

        TableColumn<StudentScheduleRow, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(new PropertyValueFactory<>("studentId"));

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

        table.getColumns().addAll(studentCol, courseCol, dayCol, slotCol, timeCol, roomCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Tab tab = new Tab("By Student", table);
        tab.setClosable(false);
        return tab;
    }

    private Tab createByDaySlotTab(Schedule schedule) {

        if (schedule == null) {
            Tab tab = new Tab("By Day/Slot", new Label("No schedule loaded. Import CSV files and run scheduling from Actions menu."));
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
            String rooms = joinRoomIds(exam.getAssignedRooms());

            items.add(new DaySlotScheduleRow(day, slotIndex, timeRange, courseCode, rooms));
        }

        items.sort(Comparator
                .comparingInt(DaySlotScheduleRow::getDay)
                .thenComparingInt(DaySlotScheduleRow::getSlotIndex)
                .thenComparing(DaySlotScheduleRow::getCourseCode));

        table.setItems(items);

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

        table.getColumns().addAll(dayCol, slotCol, timeCol, courseCol, roomCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        Tab tab = new Tab("By Day/Slot", table);
        tab.setClosable(false);
        return tab;
    }


    private String joinRoomIds(List<Classroom> rooms) {
        return rooms.stream()
                .map(Classroom::getRoomId)
                .collect(Collectors.joining(", "));
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




    public static class CourseScheduleRow {
        private final String courseCode;

        private final SimpleIntegerProperty day;
        private final SimpleIntegerProperty slotIndex;
        private final String timeRange;
        private final String rooms;
        private final int studentCount;

        public CourseScheduleRow(String courseCode, int day, int slotIndex,
                                 String timeRange, String rooms, int studentCount) {
            this.courseCode = courseCode;

            this.day = new SimpleIntegerProperty(day);
            this.slotIndex = new SimpleIntegerProperty(slotIndex);
            this.timeRange = timeRange;
            this.rooms = rooms;
            this.studentCount = studentCount;
        }

        public String getCourseCode() { return courseCode; }


        public int getDay() { return day.get(); }
        public SimpleIntegerProperty dayProperty() { return day; }
        public void setDay(int newDay) { this.day.set(newDay); } // FR7 Setter

        public int getSlotIndex() { return slotIndex.get(); }
        public SimpleIntegerProperty slotIndexProperty() { return slotIndex; }
        public void setSlotIndex(int newSlotIndex) { this.slotIndex.set(newSlotIndex); } // FR7 Setter

        public String getTimeRange() { return timeRange; }
        public String getRooms() { return rooms; }
        public int getStudentCount() { return studentCount; }
    }

    public static class StudentRow {
        private final SimpleStringProperty studentId;

        public StudentRow(String studentId) {
            this.studentId = new SimpleStringProperty(studentId);
        }

        public String getStudentId() { return studentId.get(); }
        public SimpleStringProperty studentIdProperty() { return studentId; }
    }


    public static class CourseRow {
        private final SimpleStringProperty courseCode;
        private final SimpleIntegerProperty studentCount;

        public CourseRow(String courseCode, int studentCount) {
            this.courseCode = new SimpleStringProperty(courseCode);
            this.studentCount = new SimpleIntegerProperty(studentCount);
        }

        public String getCourseCode() { return courseCode.get(); }
        public SimpleStringProperty courseCodeProperty() { return courseCode; }

        public int getStudentCount() { return studentCount.get(); }
        public SimpleIntegerProperty studentCountProperty() { return studentCount; }

        public void setStudentCount(int count) { this.studentCount.set(count); }
    }


    public static class ClassroomRow {
        private final SimpleStringProperty roomId;
        private final SimpleIntegerProperty capacity;

        public ClassroomRow(String roomId, int capacity) {
            this.roomId = new SimpleStringProperty(roomId);
            this.capacity = new SimpleIntegerProperty(capacity);
        }

        public String getRoomId() { return roomId.get(); }
        public SimpleStringProperty roomIdProperty() { return roomId; }

        public int getCapacity() { return capacity.get(); }
        public SimpleIntegerProperty capacityProperty() { return capacity; }

        public void setCapacity(int capacity) { this.capacity.set(capacity); }
    }

    public static class RegistrationRow {
        private final SimpleStringProperty studentId;
        private final SimpleStringProperty courseCode;

        public RegistrationRow(String studentId, String courseCode) {
            this.studentId = new SimpleStringProperty(studentId);
            this.courseCode = new SimpleStringProperty(courseCode);
        }

        public String getStudentId() { return studentId.get(); }
        public SimpleStringProperty studentIdProperty() { return studentId; }

        public String getCourseCode() { return courseCode.get(); }
        public SimpleStringProperty courseCodeProperty() { return courseCode; }
    }

    private void applyRowToSchedule(CourseScheduleRow row) {
        if (schedule == null) return;

        String courseCode = row.getCourseCode();
        Exam exam = schedule.getExamByCourse(courseCode);
        if (exam == null) return;

        int newDay = row.getDay();
        int newSlotIndex = row.getSlotIndex();

        Slot newSlot = null;
        for (Slot s : repo.getSlots()) {
            if (s.getDay() == newDay && s.getIndex() == newSlotIndex) {
                newSlot = s;
                break;
            }
        }

        if (newSlot == null) {
            showError("Invalid Slot", "Day " + newDay + ", Slot " + newSlotIndex + " için slot bulunamadı.");
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            return;
        }

        if (wouldCauseSameSlotStudentConflict(exam, newSlot)) {
            showError("Conflict", "Bu slotta öğrencileri çakışan başka sınav var (FR10 ihlali).");
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            return;
        }

        if (wouldCauseRoomConflict(exam, newSlot)) {
            showError("Conflict", "Bu slotta aynı sınıf(lar) başka bir sınav tarafından kullanılıyor (room conflict).");
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            return;
        }

        if (wouldViolateConsecutiveConstraint(exam, newSlot)) {
            showError("Constraint Violation", "Bu değişiklik bazı öğrenciler için ardışık (consecutive) sınav oluşturuyor.");
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            return;
        }

        if (wouldViolateMaxTwoPerDayConstraint(exam, newSlot)) {
            showError("Constraint Violation", "Bu değişiklik bazı öğrenciler için bir günde 2'den fazla sınav oluşturuyor.");
            row.setDay(exam.getSlot().getDay());
            row.setSlotIndex(exam.getSlot().getIndex());
            return;
        }

        exam.setSlot(newSlot);
        Platform.runLater(this::updateAllViews);
    }

    private boolean wouldCauseSameSlotStudentConflict(Exam movingExam, Slot newSlot) {
        Set<String> movingStudents = new HashSet<>(movingExam.getCourse().getStudentIds());

        for (Exam other : schedule.getAllExams()) {
            if (other == movingExam) continue;

            if (other.getSlot().getDay() == newSlot.getDay()
                    && other.getSlot().getIndex() == newSlot.getIndex()) {

                for (String s : movingStudents) {
                    if (other.getCourse().getStudentIds().contains(s)) {
                        return true; // aynı slotta ortak öğrenci var
                    }
                }
            }
        }
        return false;
    }

    private boolean wouldCauseRoomConflict(Exam movingExam, Slot newSlot) {
        Set<String> movingRooms = new HashSet<>();
        for (Classroom cr : movingExam.getAssignedRooms()) {
            movingRooms.add(cr.getRoomId());
        }

        for (Exam other : schedule.getAllExams()) {
            if (other == movingExam) continue;

            if (other.getSlot().getDay() == newSlot.getDay()
                    && other.getSlot().getIndex() == newSlot.getIndex()) {

                for (Classroom cr : other.getAssignedRooms()) {
                    if (movingRooms.contains(cr.getRoomId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean wouldViolateConsecutiveConstraint(Exam movingExam, Slot newSlot) {
        int day = newSlot.getDay();
        int idx = newSlot.getIndex();
        int prev = idx - 1;
        int next = idx + 1;

        for (String studentId : movingExam.getCourse().getStudentIds()) {
            for (Exam other : schedule.getAllExams()) {
                if (other == movingExam) continue;
                if (!other.getCourse().getStudentIds().contains(studentId)) continue;
                if (other.getSlot().getDay() != day) continue;

                int oIdx = other.getSlot().getIndex();
                if (oIdx == prev || oIdx == next) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean wouldViolateMaxTwoPerDayConstraint(Exam movingExam, Slot newSlot) {
        int day = newSlot.getDay();

        for (String studentId : movingExam.getCourse().getStudentIds()) {
            int count = 0;
            for (Exam other : schedule.getAllExams()) {
                if (other == movingExam) continue;
                if (!other.getCourse().getStudentIds().contains(studentId)) continue;
                if (other.getSlot().getDay() == day) {
                    count++;
                    if (count >= 2) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
    public static class RoomScheduleRow {
        private final String roomId;
        private final int day;
        private final int slotIndex;
        private final String timeRange;
        private final String courseCode;

        public RoomScheduleRow(String roomId, int day, int slotIndex,
                               String timeRange, String courseCode) {
            this.roomId = roomId;
            this.day = day;
            this.slotIndex = slotIndex;
            this.timeRange = timeRange;
            this.courseCode = courseCode;
        }

        public String getRoomId() { return roomId; }
        public int getDay() { return day; }
        public int getSlotIndex() { return slotIndex; }
        public String getTimeRange() { return timeRange; }
        public String getCourseCode() { return courseCode; }
    }

    public static class StudentScheduleRow {
        private final String studentId;
        private final String courseCode;
        private final int day;
        private final int slotIndex;
        private final String timeRange;
        private final String rooms;

        public StudentScheduleRow(String studentId, String courseCode,
                                  int day, int slotIndex,
                                  String timeRange, String rooms) {
            this.studentId = studentId;
            this.courseCode = courseCode;
            this.day = day;
            this.slotIndex = slotIndex;
            this.timeRange = timeRange;
            this.rooms = rooms;
        }

        public String getStudentId() { return studentId; }
        public String getCourseCode() { return courseCode; }
        public int getDay() { return day; }
        public int getSlotIndex() { return slotIndex; }
        public String getTimeRange() { return timeRange; }
        public String getRooms() { return rooms; }
    }

    public static class DaySlotScheduleRow {
        private final int day;
        private final int slotIndex;
        private final String timeRange;
        private final String courseCode;
        private final String rooms;

        public DaySlotScheduleRow(int day, int slotIndex,
                                  String timeRange, String courseCode, String rooms) {
            this.day = day;
            this.slotIndex = slotIndex;
            this.timeRange = timeRange;
            this.courseCode = courseCode;
            this.rooms = rooms;
        }

        public int getDay() { return day; }
        public int getSlotIndex() { return slotIndex; }
        public String getTimeRange() { return timeRange; }
        public String getCourseCode() { return courseCode; }
        public String getRooms() { return rooms; }

    }

    public static class SlotConfigurationRow {
        private final SimpleIntegerProperty day;
        private final SimpleIntegerProperty slotIndex;
        private final SimpleStringProperty startTime;
        private final SimpleStringProperty endTime;

        public SlotConfigurationRow(int day, int slotIndex, String startTime, String endTime) {
            this.day = new SimpleIntegerProperty(day);
            this.slotIndex = new SimpleIntegerProperty(slotIndex);
            this.startTime = new SimpleStringProperty(startTime);
            this.endTime = new SimpleStringProperty(endTime);
        }

        public int getDay() { return day.get(); }
        public SimpleIntegerProperty dayProperty() { return day; }
        public void setDay(int day) { this.day.set(day); }

        public int getSlotIndex() { return slotIndex.get(); }
        public SimpleIntegerProperty slotIndexProperty() { return slotIndex; }

        public String getStartTime() { return startTime.get(); }
        public SimpleStringProperty startTimeProperty() { return startTime; }
        public void setStartTime(String startTime) { this.startTime.set(startTime); }

        public String getEndTime() { return endTime.get(); }
        public SimpleStringProperty endTimeProperty() { return endTime; }
        public void setEndTime(String endTime) { this.endTime.set(endTime); }
    }

    public static void main(String[] args) {
        launch(args);
    }
}