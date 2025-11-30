import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        DataRepository repo = new DataRepository();
        try {
            repo.loadAll(
                    Paths.get("sampleData_AllStudents.csv"),
                    Paths.get("sampleData_AllCourses.csv"),
                    Paths.get("sampleData_AllClassroomsAndTheirCapacities.csv"),
                    Paths.get("sampleData_AllAttendanceLists.csv")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Courses: " + repo.getCourses().size());
        System.out.println("Students: " + repo.getStudents().size());
        System.out.println("Classrooms: " + repo.getClassrooms().size());
    }
}
