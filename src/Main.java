import java.io.IOException;
import java.nio.file.Paths;

public class Main {

    public static void main(String[] args) {

        DataRepository repo = new DataRepository();
        CsvImportService importService = new CsvImportService(repo);

        try {

            importService.importAll(
                    Paths.get("sampleData_AllStudents.csv"),
                    Paths.get("sampleData_AllCourses.csv"),
                    Paths.get("sampleData_AllClassroomsAndTheirCapacities.csv"),
                    Paths.get("sampleData_AllAttendanceLists.csv"),
                    Paths.get("sampleData_slot_config.csv")
            );

            System.out.println("Courses: " + repo.getCourses().size());
            System.out.println("Students: " + repo.getStudents().size());
            System.out.println("Classrooms: " + repo.getClassrooms().size());
            System.out.println("Slots: " + repo.getSlots().size());


            SchedulingEngine engine = new SchedulingEngine(repo);
            Schedule schedule = engine.generateExamSchedule();


            CsvExportService exportService = new CsvExportService(repo);
            exportService.exportByCourse(schedule, Paths.get("schedule_by_course.csv"));
            exportService.exportByRoom(schedule, Paths.get("schedule_by_room.csv"));
            exportService.exportByStudent(schedule, Paths.get("schedule_by_student.csv"));
            exportService.exportByDaySlot(schedule, Paths.get("schedule_by_day_slot.csv"));

            System.out.println("Export completed.");

        } catch (IOException e) {
            throw new RuntimeException("Error while importing/exporting CSV data", e);
        }
    }
}
