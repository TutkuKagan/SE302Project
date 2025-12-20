import java.util.List;

public class Exam {

    private Course course;
    private Slot slot;
    private List<Classroom> assignedRooms;

    public Exam(Course course, Slot slot, List<Classroom> assignedRooms) {
        this.course = course;
        this.slot = slot;
        this.assignedRooms = assignedRooms;
    }

    public Course getCourse() {
        return course;
    }

    public Slot getSlot() {
        return slot;
    }

    public void setSlot(Slot slot) { this.slot = slot; }

    public List<Classroom> getAssignedRooms() {
        return assignedRooms;
    }
}
