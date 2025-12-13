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
        } catch (IOException e) {
            throw new RuntimeException("Error while importing CSV data", e);
        }

        System.out.println("Courses: " + repo.getCourses().size());
        System.out.println("Students: " + repo.getStudents().size());
        System.out.println("Classrooms: " + repo.getClassrooms().size());
        System.out.println("Slots: " + repo.getSlots().size());
    }
}
