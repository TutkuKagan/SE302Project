import java.util.ArrayList;
import java.util.List;

public class Course {
    private String courseCode;
    private List<String> studentIds = new ArrayList<>();

    public Course(String courseCode) {
        this.courseCode = courseCode;
    }

    public void addStudent(String studentId) { studentIds.add(studentId); }

    public String getCourseCode() { return courseCode; }
    public List<String> getStudentIds() { return studentIds; }
}
