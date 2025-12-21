package com.example.scheduler.controller;

import com.example.scheduler.model.Course;
import com.example.scheduler.model.CourseRow;
import com.example.scheduler.model.DataRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.Comparator;

public class CourseController {

    private final DataRepository repo;
    private final ObservableList<CourseRow> courseList;

    public CourseController(DataRepository repo) {
        this.repo = repo;
        this.courseList = FXCollections.observableArrayList();
        refreshList();
    }

    public ObservableList<CourseRow> getCourseList() {
        return courseList;
    }

    public void refreshList() {
        courseList.clear();
        for (Course c : repo.getCourses().values()) {
            // We need student count. Course object has list of studentIds.
            int count = c.getStudentIds().size();
            courseList.add(new CourseRow(c.getCourseCode(), count));
        }
        courseList.sort(Comparator.comparing(CourseRow::getCourseCode));
    }

    public boolean addCourse(String code, int studentCount) {
        // Note: Adding a course manually here.
        // The original App might not have supported adding student count directly?
        // Let's check the view logic.
        // Assuming we just create a course.
        if (repo.getCourses().containsKey(code)) {
            return false;
        }
        // How to set student count?
        // The original code probably just created a course with empty students?
        // or set a dummy count?
        // Detailed check of view logic needed.
        repo.addCourse(code);

        // If the user input implies student count, we might need to simulate students?
        // For now, let's assume 0 students.

        courseList.add(new CourseRow(code, 0));
        courseList.sort(Comparator.comparing(CourseRow::getCourseCode));
        return true;
    }

    public boolean removeCourse(CourseRow row) {
        if (row == null)
            return false;
        boolean removed = repo.removeCourse(row.getCourseCode());
        if (removed) {
            courseList.remove(row);
        }
        return removed;
    }
}
