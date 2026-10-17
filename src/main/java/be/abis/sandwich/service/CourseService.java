package be.abis.sandwich.service;

import be.abis.sandwich.model.Course;

import java.util.List;

public interface CourseService {

    /** Returns the course with this title; creates it first if it doesn't exist yet. */
    Course findOrCreateCourse(String title);

    /** All courses, sorted by title. */
    List<Course> findAllCourses();
}
