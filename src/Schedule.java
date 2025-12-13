import java.util.*;


public class Schedule {


    private final Map<String, Exam> examsByCourse = new HashMap<>();

    public void addExam(Exam exam) {
        String code = exam.getCourse().getCourseCode();
        examsByCourse.put(code, exam);
    }

    public Collection<Exam> getAllExams() {
        return examsByCourse.values();
    }

    public Exam getExamForCourse(String courseCode) {
        return examsByCourse.get(courseCode);
    }
}
