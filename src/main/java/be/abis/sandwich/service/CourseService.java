package be.abis.sandwich.service;

import be.abis.sandwich.model.Course;

public interface CourseService {

    /** Returns the course with this title; creates it first if it doesn't exist yet. */
    Course findOrCreateCourse(String title);
}
