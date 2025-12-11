import java.util.*;

public class SchedulingEngine {

    private DataRepository repo;

    public SchedulingEngine(DataRepository repo) {
        this.repo = repo;
    }

    // Chooses one or more rooms whose combined capacity >= number of students

    public List<Classroom> assignRoomsForCourse(Course course) {
        int needed = course.getStudentCount();
        List<Classroom> rooms = new ArrayList<>(repo.getClassrooms());

        rooms.sort(Comparator.comparingInt(Classroom::getCapacity).reversed());

        List<Classroom> assigned = new ArrayList<>();
        int total = 0;

        for (Classroom room : rooms) {
            assigned.add(room);
            total += room.getCapacity();
            if (total >= needed) break;
        }

        if (total < needed) return null; // Not enough capacity

        return assigned;
    }
    private boolean sameStudentConflict(Exam a, Exam b) {
        Course c1 = a.getCourse();
        Course c2 = b.getCourse();

        for (String s : c1.getStudentIds()) {
            if (c2.getStudentIds().contains(s)) {
                return true;
            }
        }
        return false;
    }
    private boolean violatesConsecutiveRule(Exam a, Exam b) {
        if (!sameStudentConflict(a, b)) return false;

        int dayA = a.getSlot().getDay();
        int dayB = b.getSlot().getDay();
        int idxA = a.getSlot().getIndex();
        int idxB = b.getSlot().getIndex();

        if (dayA == dayB) {
            return Math.abs(idxA - idxB) == 1;
        }
        return false;
    }
    private boolean violatesMaxTwoPerDay(Exam candidate, Map<String, Exam> schedule) { //“A student cannot have more than 2 exams in one day.”
        Course newCourse = candidate.getCourse();
        int day = candidate.getSlot().getDay();

        for (String student : newCourse.getStudentIds()) {
            int count = 0;

            for (Exam e : schedule.values()) {
                if (e.getSlot().getDay() == day &&
                        e.getCourse().getStudentIds().contains(student)) {

                    count++;
                    if (count >= 2) return true; //Adding this exam would cause the student to have 3 exams on the same day (violates max-2-exams-per-day rule)
                }
            }
        }
        return false;
    }
    private boolean sameSlotStudentConflict(Exam a, Exam b) {
        if (!a.getSlot().equals(b.getSlot())) return false;
        return sameStudentConflict(a, b);
    }
    public Map<String, Exam> generateExamSchedule() {

        Map<String, Exam> schedule = new HashMap<>();

        List<Slot> slots = repo.getSlots();
        List<Course> courses = new ArrayList<>(repo.getCourses().values());

        for (Course course : courses) {
            boolean placed = false;
            for (Slot slot : slots) {
                // assign rooms
                List<Classroom> rooms = assignRoomsForCourse(course);
                if (rooms == null) {
                    throw new RuntimeException("Not enough capacity for course " + course.getCourseCode());
                }

                Exam candidate = new Exam(course, slot, rooms);

                boolean conflict = false;

                for (Exam existing : schedule.values()) {

                    if (sameSlotStudentConflict(candidate, existing) ||
                            violatesConsecutiveRule(candidate, existing) ||
                            violatesMaxTwoPerDay(candidate, schedule)) {

                        conflict = true;
                        break;
                    }
                }

                if (!conflict) {
                    schedule.put(course.getCourseCode(), candidate);
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                throw new RuntimeException("No feasible slot found for " + course.getCourseCode());
            }
        }

        return schedule;
    }


}
