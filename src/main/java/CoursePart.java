import java.util.ArrayList;
import java.util.List;

public class CoursePart {

    private Course course;
    private Slot slot;
    private Classroom room;
    private List<String> studentIds = new ArrayList<>();

    public CoursePart(Course course, Slot slot, Classroom room) {
        this.course = course;
        this.slot = slot;
        this.room = room;
    }

    public Course getCourse() { return course; }
    public Slot getSlot() { return slot; }
    public Classroom getRoom() { return room; }
    public List<String> getStudentIds() { return studentIds; }

    public void addStudent(String studentId) {
        studentIds.add(studentId);
    }
}
