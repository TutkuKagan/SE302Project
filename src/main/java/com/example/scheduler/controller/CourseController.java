package com.example.scheduler.controller;

import com.example.scheduler.model.Course;
import com.example.scheduler.model.CourseRow;
import com.example.scheduler.model.DataRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


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

        if (code == null || code.trim().isEmpty()) return false;
        code = code.trim();


        if (repo.getCourses().containsKey(code)) {
            return false;
        }
            //WE ARE ADDING RANDOM STUDENTS TO THE NEWLY ADDED COURSE.
        repo.addCourse(code);

        int n = Math.max(0, studentCount); // in the usage part, we are setting count as 40. We could implement a manual input for the count if needed
        List<String> ids = new ArrayList<>(repo.getStudents().keySet());
        Collections.shuffle(ids);

        int take = Math.min(n, ids.size());
        for (int i = 0; i < take; i++) {
            repo.registerStudentToCourse(ids.get(i), code);
        }


        courseList.add(new CourseRow(code, take));
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
