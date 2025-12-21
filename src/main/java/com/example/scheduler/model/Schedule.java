package com.example.scheduler.model;

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

    public Exam getExamByCourse(String courseCode) {
        return examsByCourse.get(courseCode);
    }
}
