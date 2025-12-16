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
import javafx.scene.control.Alert.AlertType;

import java.io.IOException;
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

    private DataRepository repo;

    private ObservableList<SlotConfigurationRow> slotData;
    private Spinner<Integer> dayCountSpinner;

    @Override
    public void start(Stage primaryStage) {
        repo = new DataRepository();

        CsvImportService importService = new CsvImportService(repo);
        try {
            importService.importAll(
                    Paths.get("sampleData_AllStudents.csv"),
                    Paths.get("sampleData_AllCourses.csv"),
                    Paths.get("sampleData_AllClassroomsAndTheirCapacities.csv"),
                    Paths.get("sampleData_AllAttendanceLists.csv"),
                    Paths.get("sampleData_slot_config.csv")
            );
        } catch (IOException e) {
            showError("CSV Import Error",
                    "An error occurred while importing CSV files:\n" + e.getMessage());
            e.printStackTrace();
            return;
        }

        SchedulingEngine engine = new SchedulingEngine(repo);
        Schedule schedule;
        try {
            schedule = engine.generateExamSchedule();
        } catch (RuntimeException ex) {
            showError("Scheduling Error",
                    "No feasible schedule could be generated:\n" + ex.getMessage());
            ex.printStackTrace();
            return;
        }

        TabPane tabPane = new TabPane();


        tabPane.getTabs().add(createSlotConfigurationTab());


        tabPane.getTabs().add(createByCourseTab(schedule));
        tabPane.getTabs().add(createByRoomTab(schedule));
        tabPane.getTabs().add(createByStudentTab(schedule));
        tabPane.getTabs().add(createByDaySlotTab(schedule));

        BorderPane root = new BorderPane(tabPane);

        Scene scene = new Scene(root, 1100, 650);
        primaryStage.setTitle("Desktop Exam Scheduling Assistant - Schedule Views (FR12)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }



    private Tab createSlotConfigurationTab() {
        VBox mainLayout = new VBox(20);
        mainLayout.setPadding(new Insets(20));

        Label title = new Label("📅 Slot ve Gün Yapılandırması (FR4)");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");


        HBox controls = createSlotControls();

        // tablo
        TableView<SlotConfigurationRow> slotTable = createSlotTable();
        loadInitialSlotData(slotTable);


        mainLayout.getChildren().addAll(title, controls, slotTable);

        Tab tab = new Tab("2. Slot Config (FR4)", mainLayout);
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



        TableColumn<SlotConfigurationRow, Integer> dayCol = new TableColumn<>("Day");
        dayCol.setCellValueFactory(new PropertyValueFactory<>("day"));
        dayCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        dayCol.setOnEditCommit(event -> {
            if (event.getNewValue() != null && event.getNewValue() > 0) {
                event.getTableView().getItems().get(event.getTablePosition().getRow()).setDay(event.getNewValue());
            } else {
                table.refresh();
            }
        });

        TableColumn<SlotConfigurationRow, Integer> slotIndexCol = new TableColumn<>("Slot Index");
        slotIndexCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

        TableColumn<SlotConfigurationRow, String> startTimeCol = new TableColumn<>("Start Time (HH:MM)");
        startTimeCol.setCellValueFactory(new PropertyValueFactory<>("startTime"));
        startTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        startTimeCol.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setStartTime(event.getNewValue());
        });

        TableColumn<SlotConfigurationRow, String> endTimeCol = new TableColumn<>("End Time (HH:MM)");
        endTimeCol.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        endTimeCol.setCellFactory(TextFieldTableCell.forTableColumn());
        endTimeCol.setOnEditCommit(event -> {
            event.getTableView().getItems().get(event.getTablePosition().getRow()).setEndTime(event.getNewValue());
        });

        // silme işlemi
        TableColumn<SlotConfigurationRow, Void> actionCol = new TableColumn<>("Action");
        actionCol.setCellFactory(param -> new TableCell<SlotConfigurationRow, Void>() {
            private final Button deleteButton = new Button("🗑️ Sil");
            {
                deleteButton.setOnAction(event -> {
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

    private void loadInitialSlotData(TableView<SlotConfigurationRow> table) {

        table.getItems().addAll(
                new SlotConfigurationRow(1, 1, "09:00", "11:00"),
                new SlotConfigurationRow(1, 2, "13:00", "15:00"),
                new SlotConfigurationRow(2, 1, "09:00", "11:00")
        );
    }

    private void addNewSlotRow() {
        // yeni satır için gün slot ekleme
        int maxDay = slotData.stream().mapToInt(SlotConfigurationRow::getDay).max().orElse(1);
        int nextSlotIndex = slotData.stream()
                .filter(s -> s.getDay() == maxDay)
                .mapToInt(SlotConfigurationRow::getSlotIndex)
                .max().orElse(0) + 1;

        SlotConfigurationRow newRow = new SlotConfigurationRow(maxDay, nextSlotIndex, "00:00", "00:00");
        slotData.add(newRow);
    }

    private void handleSaveConfiguration() {
        int numDays = dayCountSpinner.getValue();



        showInfo("Kaydedildi", "Slot konfigürasyonu başarıyla kaydedildi. Gün Sayısı: " + numDays);
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }




    private Tab createByCourseTab(Schedule schedule) {
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
            int newDay = event.getNewValue();

            if (newDay > 0) {
                row.setDay(newDay);
            } else {
                table.refresh(); // bug olursa eski hali
            }
        });

        //
        TableColumn<CourseScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));
        slotCol.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        slotCol.setOnEditCommit(event -> {
            CourseScheduleRow row = event.getRowValue();
            int newSlotIndex = event.getNewValue();

            if (newSlotIndex > 0) {
                row.setSlotIndex(newSlotIndex);

            } else {
                table.refresh(); // bug olursa eski hali
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