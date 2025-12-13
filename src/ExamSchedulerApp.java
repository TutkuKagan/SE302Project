import javafx.application.Application;
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

public class ExamSchedulerApp extends Application {

    private DataRepository repo;

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

    private Tab createByCourseTab(Schedule schedule) {
        TableView<CourseScheduleRow> table = new TableView<>();
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

        TableColumn<CourseScheduleRow, Integer> slotCol = new TableColumn<>("Slot");
        slotCol.setCellValueFactory(new PropertyValueFactory<>("slotIndex"));

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
        private final int day;
        private final int slotIndex;
        private final String timeRange;
        private final String rooms;
        private final int studentCount;

        public CourseScheduleRow(String courseCode, int day, int slotIndex,
                                 String timeRange, String rooms, int studentCount) {
            this.courseCode = courseCode;
            this.day = day;
            this.slotIndex = slotIndex;
            this.timeRange = timeRange;
            this.rooms = rooms;
            this.studentCount = studentCount;
        }

        public String getCourseCode() { return courseCode; }
        public int getDay() { return day; }
        public int getSlotIndex() { return slotIndex; }
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

    public static void main(String[] args) {
        launch(args);
    }
}