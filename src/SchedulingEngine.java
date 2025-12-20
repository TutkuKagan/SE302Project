import java.util.*;

public class SchedulingEngine {

    private final DataRepository repo;
    private SchedulingConfig config = new SchedulingConfig();


    public SchedulingEngine(DataRepository repo) {
        this.repo = repo;
    }

    public List<SchedulingResult> generateRankedSolutions() {
        List<SchedulingResult> results = new ArrayList<>();
        try {
            results.add(attemptScheduling());
        } catch (RuntimeException ignored) {
            // no feasible schedule
        }
        return results;
    }

    private SchedulingResult attemptScheduling() {
        Schedule schedule = new Schedule();
        SchedulingResult result = new SchedulingResult(schedule);

        List<Slot> slots = repo.getSlots();
        List<Course> courses = new ArrayList<>(repo.getCourses().values());

        courses.sort(Comparator.comparingInt(Course::getStudentCount).reversed());

        for (Course course : courses) {
            boolean placed = false;

            for (Slot slot : slots) {
                List<Classroom> rooms = assignRoomsForCourse(course);
                if (rooms == null) continue;

                Exam candidate = new Exam(course, slot, rooms);

                boolean hardConflict = false;
                boolean consecutiveViolation = false;

                for (Exam existing : schedule.getAllExams()) {
                    if (sameSlotStudentConflict(candidate, existing) || roomOccupancyConflict(candidate, existing)) {
                        hardConflict = true;
                        break;
                    }
                    if (violatesConsecutiveRule(candidate, existing)) {
                        consecutiveViolation = true;
                        break;
                    }
                }

                if (hardConflict) continue;
                if (consecutiveViolation) continue;

                boolean maxTwoViolation = violatesMaxTwoPerDay(candidate, schedule);
                if (maxTwoViolation) continue;

                schedule.addExam(candidate);
                placed = true;
                break;
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
        if (config.allowConsecutiveSlots) return false;
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
                    if (!config.allowThreePerDay && count >= 2) return true;
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

    public List<RelaxationSuggestion> suggestRelaxations() {

        List<RelaxationSuggestion> suggestions = new ArrayList<>();

        for (RelaxationType type : RelaxationType.values()) {

            SchedulingConfig relaxed = new SchedulingConfig();

            if (type == RelaxationType.ALLOW_CONSECUTIVE_SLOTS) {
                relaxed.allowConsecutiveSlots = true;
            }

            if (type == RelaxationType.ALLOW_THREE_PER_DAY) {
                relaxed.allowThreePerDay = true;
            }

            try {
                this.config = relaxed;
                Schedule schedule = generateExamSchedule();

                suggestions.add(new RelaxationSuggestion(
                        type,
                        explanation(type),
                        1
                ));

            } catch (RuntimeException ignored) {

            }
        }

        return suggestions;
    }

    private String explanation(RelaxationType type) {
        switch (type) {
            case ALLOW_CONSECUTIVE_SLOTS:
                return "Allow students to take exams in consecutive slots.";
            case ALLOW_THREE_PER_DAY:
                return "Allow students to have more than two exams per day.";
            default:
                return "";
        }
    }


}
