import java.util.*;

public class SchedulingEngine {

    private DataRepository repo;

    public SchedulingEngine(DataRepository repo) {
        this.repo = repo;
    }

    public List<SchedulingResult> generateRankedSolutions() {
        List<SchedulingResult> results = new ArrayList<>();

        try {
            results.add(attemptScheduling(false, false));
        } catch (RuntimeException e) { }

        try {
            results.add(attemptScheduling(true, false));
        } catch (RuntimeException e) { }

        try {
            results.add(attemptScheduling(false, true));
        } catch (RuntimeException e) { }

        try {
            results.add(attemptScheduling(true, true));
        } catch (RuntimeException e) { }

        results.sort(Comparator.comparingInt(SchedulingResult::getPenaltyScore));
        return results;
    }

    private SchedulingResult attemptScheduling(boolean relaxMaxTwo, boolean relaxConsecutive) {
        Schedule schedule = new Schedule();
        SchedulingResult result = new SchedulingResult(schedule);

        List<Slot> slots = repo.getSlots();
        List<Course> courses = new ArrayList<>(repo.getCourses().values());

        for (Course course : courses) {
            boolean placed = false;

            for (Slot slot : slots) {
                List<Classroom> rooms = assignRoomsForCourse(course);
                if (rooms == null) continue;

                Exam candidate = new Exam(course, slot, rooms);

                boolean hardConflict = false;
                for (Exam existing : schedule.getAllExams()) {
                    if (sameSlotStudentConflict(candidate, existing) || roomOccupancyConflict(candidate, existing)) {
                        hardConflict = true;
                        break;
                    }
                }
                if (hardConflict) continue;

                boolean consecutiveViolation = false;
                for (Exam existing : schedule.getAllExams()) {
                    if (violatesConsecutiveRule(candidate, existing)) {
                        consecutiveViolation = true;
                        break;
                    }
                }

                boolean maxTwoViolation = violatesMaxTwoPerDay(candidate, schedule);

                boolean canPlace = true;
                if (consecutiveViolation && !relaxConsecutive) canPlace = false;
                if (maxTwoViolation && !relaxMaxTwo) canPlace = false;

                if (canPlace) {
                    if (consecutiveViolation) {
                        result.addRelaxation("Relaxed consecutive exam rule for " + course.getCourseCode(), 50);
                    }
                    if (maxTwoViolation) {
                        result.addRelaxation("Relaxed max 2 exams per day rule for " + course.getCourseCode(), 100);
                    }

                    schedule.addExam(candidate);
                    placed = true;
                    break;
                }
            }

            if (!placed) {
                throw new RuntimeException("No feasible slot found for " + course.getCourseCode());
            }
        }
        return result;
    }

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
        return (total < needed) ? null : assigned;
    }

    private boolean sameStudentConflict(Exam a, Exam b) {
        for (String s : a.getCourse().getStudentIds()) {
            if (b.getCourse().getStudentIds().contains(s)) return true;
        }
        return false;
    }

    private boolean violatesConsecutiveRule(Exam a, Exam b) {
        if (!sameStudentConflict(a, b)) return false;
        return (a.getSlot().getDay() == b.getSlot().getDay()) &&
                (Math.abs(a.getSlot().getIndex() - b.getSlot().getIndex()) == 1);
    }

    private boolean roomOccupancyConflict(Exam a, Exam b) {
        if (a.getSlot().getDay() != b.getSlot().getDay() || a.getSlot().getIndex() != b.getSlot().getIndex()) return false;
        for (Classroom r1 : a.getAssignedRooms()) {
            for (Classroom r2 : b.getAssignedRooms()) {
                if (r1.getRoomId().equals(r2.getRoomId())) return true;
            }
        }
        return false;
    }

    private boolean violatesMaxTwoPerDay(Exam candidate, Schedule schedule) {
        int day = candidate.getSlot().getDay();
        for (String student : candidate.getCourse().getStudentIds()) {
            int count = 0;
            for (Exam e : schedule.getAllExams()) {
                if (e.getSlot().getDay() == day && e.getCourse().getStudentIds().contains(student)) {
                    count++;
                    if (count >= 2) return true;
                }
            }
        }
        return false;
    }

    private boolean sameSlotStudentConflict(Exam a, Exam b) {
        return a.getSlot().equals(b.getSlot()) && sameStudentConflict(a, b);
    }

    public Schedule generateExamSchedule() {
        List<SchedulingResult> solutions = generateRankedSolutions();
        if (solutions.isEmpty()) throw new RuntimeException("Could not generate any feasible schedule.");
        return solutions.get(0).getSchedule();
    }
}